package com.resonate.player.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.resonate.player.data.db.AlbumEntity
import com.resonate.player.data.db.ArtistEntity
import com.resonate.player.data.db.FolderEntity
import com.resonate.player.data.db.GenreEntity
import com.resonate.player.data.db.ResonateDatabase
import com.resonate.player.data.db.SongEntity
import com.resonate.player.data.db.replaceLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * §6.5: one MediaStore cursor pass, aggregates built in memory, batched Room
 * transaction. Never opens a MediaMetadataRetriever per file.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ResonateDatabase,
) {

    suspend fun fullScan(
        minDurationSec: Int,
        excludedFolders: List<String> = emptyList(),
        onProgress: (found: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val songs = ArrayList<SongEntity>(1024)
        val albums = HashMap<Long, AlbumAggregate>()
        val artists = HashMap<Long, ArtistAggregate>()
        val folders = HashMap<Long, FolderAggregate>()
        val genres = HashMap<Long, GenreAggregate>()

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ARTIST_ID)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= 29) add(MediaStore.Audio.Media.BITRATE)
            if (Build.VERSION.SDK_INT >= 30) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.GENRE_ID)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
                add(MediaStore.Audio.Media.DISC_NUMBER)
                add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
            }
        }.toTypedArray()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?",
            arrayOf((minDurationSec * 1000L).toString()),
            null,
        )?.use { cursor ->
            val idx = ColumnIndices(cursor)
            while (cursor.moveToNext()) {
                val song = cursor.toSong(idx) ?: continue
                // §2.7: excluded folders (and their subfolders) never enter the index.
                val fullPath = folderPathOf(idx, cursor)
                if (excludedFolders.any { fullPath == it || fullPath.startsWith("$it/") }) continue
                songs += song

                albums.getOrPut(song.albumId) {
                    AlbumAggregate(song.albumName, song.artistId, song.artistName)
                }.also {
                    it.songCount++
                    if (song.year > it.year) it.year = song.year
                }
                artists.getOrPut(song.artistId) { ArtistAggregate(song.artistName) }.also {
                    it.songCount++
                    it.albumIds += song.albumId
                }
                folders.getOrPut(song.folderId) {
                    FolderAggregate(folderPathOf(idx, cursor), song.relativePath)
                }.also { it.songCount++ }
                val genreId = song.genreId
                if (genreId != null) {
                    genres.getOrPut(genreId) { GenreAggregate(idx.genreName(cursor)) }
                        .also { it.songCount++ }
                }

                if (songs.size % 250 == 0) onProgress(songs.size)
            }
        }
        onProgress(songs.size)

        db.replaceLibrary(
            songs = songs,
            albums = albums.map { (id, a) ->
                AlbumEntity(
                    id = id,
                    name = a.name,
                    artistId = a.artistId,
                    artistName = a.artistName,
                    year = a.year,
                    songCount = a.songCount,
                    artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, id).toString(),
                    chromaPrimary = null,
                    chromaSecondary = null,
                    chromaOnColor = null,
                )
            },
            artists = artists.map { (id, a) ->
                ArtistEntity(id = id, name = a.name, albumCount = a.albumIds.size, songCount = a.songCount)
            },
            folders = folders.map { (id, f) ->
                FolderEntity(id = id, path = f.path, name = f.path.substringAfterLast('/'), songCount = f.songCount)
            },
            genres = genres.map { (id, g) ->
                GenreEntity(id = id, name = g.name, songCount = g.songCount)
            },
        )
        songs.size
    }

    private class ColumnIndices(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val track = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModified = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeType = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val data = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val displayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val bitrate = cursor.getColumnIndex("bitrate")
        val genre = cursor.getColumnIndex("genre")
        val genreId = cursor.getColumnIndex("genre_id")
        val discNumber = cursor.getColumnIndex("disc_number")
        val cdTrackNumber = cursor.getColumnIndex("cd_track_number")

        fun genreName(cursor: Cursor): String =
            if (genre >= 0) cursor.getStringOrEmpty(genre).ifEmpty { UNKNOWN } else UNKNOWN
    }

    private fun Cursor.toSong(idx: ColumnIndices): SongEntity? {
        val id = getLong(idx.id)
        val data = getStringOrEmpty(idx.data)
        if (data.isEmpty()) return null

        // TRACK packs disc*1000 + track on pre-30; prefer the real columns when present.
        val rawTrack = getInt(idx.track)
        val disc = when {
            idx.discNumber >= 0 && !isNull(idx.discNumber) ->
                getStringOrEmpty(idx.discNumber).substringBefore('/').toIntOrNull() ?: 1
            rawTrack >= 1000 -> rawTrack / 1000
            else -> 1
        }
        val trackNumber = when {
            idx.cdTrackNumber >= 0 && !isNull(idx.cdTrackNumber) ->
                getStringOrEmpty(idx.cdTrackNumber).substringBefore('/').toIntOrNull() ?: (rawTrack % 1000)
            else -> rawTrack % 1000
        }

        val parentPath = data.substringBeforeLast('/', missingDelimiterValue = "")
        val mime = getStringOrEmpty(idx.mimeType)

        return SongEntity(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
            title = getStringOrEmpty(idx.title).ifEmpty {
                getStringOrEmpty(idx.displayName).substringBeforeLast('.')
            },
            trackNumber = trackNumber,
            discNumber = disc,
            year = getInt(idx.year),
            durationMs = getLong(idx.duration),
            dateAddedSec = getLong(idx.dateAdded),
            dateModifiedSec = getLong(idx.dateModified),
            albumId = getLong(idx.albumId),
            artistId = getLong(idx.artistId),
            folderId = folderIdOf(parentPath),
            genreId = if (idx.genreId >= 0 && !isNull(idx.genreId)) getLong(idx.genreId) else null,
            sizeBytes = getLong(idx.size),
            mimeType = mime,
            bitrate = if (idx.bitrate >= 0 && !isNull(idx.bitrate)) getInt(idx.bitrate) else null,
            sampleRate = null, // MediaStore doesn't expose it; filled lazily if ever needed
            isSupported = isMimeSupported(mime),
            relativePath = parentPath.removePrefix(EXTERNAL_STORAGE_PREFIX).trimStart('/'),
            fileName = getStringOrEmpty(idx.displayName).ifEmpty { data.substringAfterLast('/') },
            albumName = getStringOrEmpty(idx.album).ifEmpty { UNKNOWN },
            artistName = getStringOrEmpty(idx.artist).ifEmpty { UNKNOWN },
        )
    }

    private fun folderPathOf(idx: ColumnIndices, cursor: Cursor): String =
        cursor.getStringOrEmpty(idx.data).substringBeforeLast('/', missingDelimiterValue = "")

    private class AlbumAggregate(val name: String, val artistId: Long, val artistName: String) {
        var year: Int = 0
        var songCount: Int = 0
    }

    private class ArtistAggregate(val name: String) {
        var songCount: Int = 0
        val albumIds = HashSet<Long>()
    }

    private class FolderAggregate(val path: String, val relativePath: String) {
        var songCount: Int = 0
    }

    private class GenreAggregate(val name: String) {
        var songCount: Int = 0
    }

    companion object {
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
        private const val EXTERNAL_STORAGE_PREFIX = "/storage/emulated/0"
        private const val UNKNOWN = "<unknown>"

        fun folderIdOf(path: String): Long {
            var hash = 1125899906842597L
            for (ch in path) hash = 31 * hash + ch.code
            return hash
        }

        // §4: index everything, mark what ExoPlayer can't decode so the UI can
        // dim the row instead of crashing the queue.
        private val unsupportedMimes = setOf(
            "audio/x-ms-wma",
            "audio/x-ape",
            "audio/ape",
            "audio/x-wavpack",
            "audio/wavpack",
            "audio/x-tta",
            "audio/x-dsf",
            "audio/x-dff",
            "audio/dsd",
        )

        fun isMimeSupported(mime: String): Boolean =
            mime.lowercase() !in unsupportedMimes
    }
}

private fun Cursor.getStringOrEmpty(index: Int): String =
    if (index >= 0 && !isNull(index)) getString(index) ?: "" else ""
