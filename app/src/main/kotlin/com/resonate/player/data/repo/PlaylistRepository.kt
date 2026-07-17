package com.resonate.player.data.repo

import android.content.Context
import android.net.Uri
import com.resonate.player.data.db.PlaylistDao
import com.resonate.player.data.db.PlaylistEntity
import com.resonate.player.data.db.PlaylistRow
import com.resonate.player.data.db.PlaylistSongRef
import com.resonate.player.data.db.SongEntity
import com.resonate.player.data.m3u.M3uCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class PlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
) {
    fun playlists(): Flow<List<PlaylistRow>> = playlistDao.playlistRows()

    fun songRefs(): Flow<List<PlaylistSongRef>> = playlistDao.songRefs()

    fun playlist(id: Long): Flow<PlaylistEntity?> = playlistDao.playlist(id)

    fun songsIn(playlistId: Long): Flow<List<SongEntity>> = playlistDao.songsIn(playlistId)

    suspend fun songsInOnce(playlistId: Long): List<SongEntity> = playlistDao.songsInOnce(playlistId)

    suspend fun create(name: String, songIds: List<Long> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = playlistDao.insertPlaylist(
            PlaylistEntity(id = 0, name = name, createdAt = now, updatedAt = now)
        )
        if (songIds.isNotEmpty()) playlistDao.appendSongs(id, songIds, now)
        return id
    }

    suspend fun rename(id: Long, name: String) =
        playlistDao.rename(id, name, System.currentTimeMillis())

    suspend fun delete(id: Long) = playlistDao.deletePlaylistFully(id)

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) =
        playlistDao.appendSongs(playlistId, songIds, System.currentTimeMillis())

    suspend fun setSongs(playlistId: Long, songIds: List<Long>) =
        playlistDao.setSongs(playlistId, songIds, System.currentTimeMillis())

    /** Writes the playlist to a SAF-picked document. Returns entry count. */
    suspend fun exportM3u(playlistId: Long, target: Uri): Int = withContext(Dispatchers.IO) {
        val rows = playlistDao.songPathsIn(playlistId)
        val text = M3uCodec.write(
            rows.map {
                M3uCodec.Entry(
                    path = "${it.folderPath}/${it.fileName}",
                    title = it.title,
                    artist = it.artistName,
                    durationMs = it.durationMs,
                )
            }
        )
        context.contentResolver.openOutputStream(target)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: return@withContext 0
        rows.size
    }

    /** Imports a SAF-picked M3U/M3U8. Returns matched song count, or null on read failure. */
    suspend fun importM3u(source: Uri, name: String): Int? = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(source)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return@withContext null

        val paths = M3uCodec.parse(text)
        val library = playlistDao.allSongPaths()
        val byFullPath = library.associate { "${it.folderPath}/${it.fileName}" to it.songId }
        val byFileName = library.groupBy({ it.fileName }, { it.songId })

        val matched = M3uCodec.matchToLibrary(paths, byFullPath, byFileName)
        if (matched.isNotEmpty()) create(name, matched)
        matched.size
    }
}
