package com.resonate.player.data.repo

import android.content.Context
import android.net.Uri
import com.resonate.player.data.db.PlayStatDao
import com.resonate.player.data.db.PlayStatEntity
import com.resonate.player.data.db.PlaylistDao
import com.resonate.player.data.db.SongDao
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.data.prefs.UserPrefsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wave 3 backup/restore: one JSON file with prefs, playlists, and play
 * history. Songs are referenced by uri AND file name so a backup survives
 * both reinstalls (uris stable) and device moves (file names match).
 */
@Serializable
data class BackupSong(val uri: String, val fileName: String)

@Serializable
data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val songs: List<BackupSong>,
)

@Serializable
data class BackupStat(
    val uri: String,
    val fileName: String,
    val playCount: Int,
    val lastPlayedAt: Long,
    val isFavorite: Boolean,
)

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val prefs: UserPrefs,
    val playlists: List<BackupPlaylist>,
    val stats: List<BackupStat>,
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsStore: UserPrefsStore,
    private val playlistDao: PlaylistDao,
    private val playStatDao: PlayStatDao,
    private val songDao: SongDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /** Returns true on success. */
    suspend fun export(target: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val songsById = songDao.idUriPairs().associateBy { it.id }
            val fileNameById = playlistDao.allSongPaths().associate { it.songId to it.fileName }

            val playlists = playlistDao.allPlaylistsOnce().map { playlist ->
                BackupPlaylist(
                    name = playlist.name,
                    createdAt = playlist.createdAt,
                    songs = playlistDao.songsInOnce(playlist.id).map {
                        BackupSong(uri = it.uri, fileName = it.fileName)
                    },
                )
            }
            val stats = playStatDao.backupByUri().map { stat ->
                BackupStat(
                    uri = stat.uri,
                    fileName = stat.uri.let { uri ->
                        songsById.values.firstOrNull { it.uri == uri }?.let { fileNameById[it.id] }
                    }.orEmpty(),
                    playCount = stat.playCount,
                    lastPlayedAt = stat.lastPlayedAt,
                    isFavorite = stat.isFavorite,
                )
            }
            val backup = BackupFile(
                exportedAt = System.currentTimeMillis(),
                prefs = prefsStore.prefs.first(),
                playlists = playlists,
                stats = stats,
            )
            context.contentResolver.openOutputStream(target)?.use { stream ->
                stream.write(json.encodeToString(BackupFile.serializer(), backup).toByteArray())
            } != null
        } catch (_: Exception) {
            false
        }
    }

    /** Returns restored playlist count, or null on failure. */
    suspend fun import(source: Uri): Int? = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(source)?.use {
                it.readBytes().decodeToString()
            } ?: return@withContext null
            val backup = json.decodeFromString(BackupFile.serializer(), text)

            val idByUri = songDao.idUriPairs().associate { it.uri to it.id }
            val idByFileName = playlistDao.allSongPaths()
                .groupBy({ it.fileName }, { it.songId })

            fun resolve(uri: String, fileName: String): Long? =
                idByUri[uri] ?: idByFileName[fileName]?.singleOrNull()

            // Prefs — restore everything except transient queue state.
            val current = prefsStore.prefs.first()
            prefsStore.update {
                backup.prefs.copy(
                    queueIndex = current.queueIndex,
                    queuePositionMs = current.queuePositionMs,
                )
            }

            // Playlists — recreated fresh; existing ones are left alone.
            var restored = 0
            for (playlist in backup.playlists) {
                val songIds = playlist.songs.mapNotNull { resolve(it.uri, it.fileName) }
                if (songIds.isNotEmpty()) {
                    playlistDao.insertPlaylist(
                        com.resonate.player.data.db.PlaylistEntity(
                            id = 0,
                            name = playlist.name,
                            createdAt = playlist.createdAt,
                            updatedAt = System.currentTimeMillis(),
                        )
                    ).also { newId ->
                        playlistDao.appendSongs(newId, songIds, System.currentTimeMillis())
                    }
                    restored++
                }
            }

            // Stats — merge, never losing local history (max wins).
            for (stat in backup.stats) {
                val songId = resolve(stat.uri, stat.fileName) ?: continue
                val local = playStatDao.forSong(songId)
                playStatDao.insertAll(
                    listOf(
                        PlayStatEntity(
                            songId = songId,
                            playCount = maxOf(local?.playCount ?: 0, stat.playCount),
                            lastPlayedAt = maxOf(local?.lastPlayedAt ?: 0, stat.lastPlayedAt),
                            isFavorite = (local?.isFavorite ?: false) || stat.isFavorite,
                        )
                    )
                )
            }
            restored
        } catch (_: Exception) {
            null
        }
    }
}
