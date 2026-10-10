package com.resonate.player.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.DocumentsContract
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
        learningFolderTreeUri: String? = null,
        onProgress: (found: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        if (learningFolderTreeUri != null) {
            return@withContext scanSelectedTree(
                treeUris = learningFolderTreeUri.split('\n').filter { it.isNotBlank() }.distinct().map(Uri::parse),
                minDurationSec = minDurationSec,
                excludedFolders = excludedFolders,
                onProgress = onProgress,
            )
        }
        val learningFolderPath: String? = null
        if (learningFolderTreeUri != null && learningFolderPath == null) {
            throw IllegalArgumentException("Please select a folder in Android internal shared storage.")
        }
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

        // A user-selected learning folder is an explicit audio source, so do not
        // require MediaStore's IS_MUSIC flag: recordings and other audio files in
        // the folder may be indexed by Android without being classified as music.
        val selection = if (learningFolderPath != null) {
            "${MediaStore.Audio.Media.DURATION} >= ?"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        }
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf((minDurationSec * 1000L).toString()),
            null,
        )?.use { cursor ->
            val idx = ColumnIndices(cursor)
            while (cursor.moveToNext()) {
                val fullPath = folderPathOf(idx, cursor)
                // Include the selected directory and descendants only.
                if (learningFolderPath != null && fullPath != learningFolderPath &&
                    !fullPath.startsWith("$learningFolderPath/")
                ) continue
                // Excluded folders (and their subfolders) never enter the index.
                if (excludedFolders.any { fullPath == it || fullPath.startsWith("$it/") }) continue
                val song = cursor.toSong(idx) ?: continue
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


    /**
     * Scan the selected SAF tree itself instead of relying on MediaStore's index.
     * This finds audio files in nested folders even when Android has not indexed them.
     */
    private suspend fun scanSelectedTree(
        treeUris: List<Uri>,
        minDurationSec: Int,
        excludedFolders: List<String>,
        onProgress: (found: Int) -> Unit,
    ): Int {
        val songs = ArrayList<SongEntity>()
        val visited = HashSet<String>()
        var inspected = 0

        fun walk(treeUri: Uri, rootPath: String, documentId: String, relativeParts: List<String>) {
            if (!visited.add(treeUri.toString() + "|" + documentId)) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idColumn) ?: continue
                    val name = cursor.getString(nameColumn) ?: continue
                    val mime = if (cursor.isNull(mimeColumn)) "" else cursor.getString(mimeColumn).orEmpty()
                    val childParts = relativeParts + name
                    val path = "$rootPath/" + childParts.joinToString("/")
                    if (excludedFolders.any { path == it || path.startsWith("$it/") }) continue

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walk(treeUri, rootPath, childId, childParts)
                        continue
                    }
                    if (!isAudioDocument(name, mime)) continue

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    inspected++
                    val song = runCatching {
                        songFromDocument(
                            uri = documentUri,
                            name = name,
                            mimeType = mime,
                            sizeBytes = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else 0L,
                            modifiedMillis = if (modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)) cursor.getLong(modifiedColumn) else 0L,
                            folderPath = path.substringBeforeLast('/'),
                            relativePath = childParts.dropLast(1).joinToString("/"),
                        )
                    }.getOrNull() ?: continue
                    // With the default "don't ignore" option, keep even files whose
                    // duration metadata is unavailable. A manually selected threshold
                    // excludes both short tracks and files whose duration cannot be read.
                    if (minDurationSec > 0 && song.durationMs < minDurationSec * 1000L) continue
                    songs += song
                    if (songs.size % 50 == 0) onProgress(songs.size)
                }
            } ?: throw IllegalStateException("Cannot read the selected folder. Please choose it again and grant access.")
        }

        treeUris.forEach { treeUri ->
            val rootDocumentId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (e: Exception) {
                throw IllegalArgumentException("The selected folder permission is invalid. Please select it again.", e)
            }
            val rootPath = sharedStoragePathFromTreeUri(treeUri.toString())
                ?: throw IllegalArgumentException("Please select a folder in Android internal shared storage.")
            walk(treeUri, rootPath, rootDocumentId, emptyList())
        }
        val uniqueSongs = songs.distinctBy { it.id }
        onProgress(uniqueSongs.size)

        val albums = uniqueSongs.groupBy { it.albumId }.map { (id, group) ->
            val first = group.first()
            AlbumEntity(
                id = id,
                name = first.albumName,
                artistId = first.artistId,
                artistName = first.artistName,
                year = group.maxOfOrNull { it.year } ?: 0,
                songCount = group.size,
                artworkUri = null,
                chromaPrimary = null,
                chromaSecondary = null,
                chromaOnColor = null,
            )
        }
        val artists = uniqueSongs.groupBy { it.artistId }.map { (id, group) ->
            ArtistEntity(id = id, name = group.first().artistName, albumCount = group.map { it.albumId }.distinct().size, songCount = group.size)
        }
        val folders = uniqueSongs.groupBy { it.folderId }.map { (id, group) ->
            val path = group.first().uri.let { uri ->
                // Keep a human-readable path in the folder browser, independent of document IDs.
                runCatching {
                    val documentId = DocumentsContract.getDocumentId(Uri.parse(uri))
                    val relative = documentId.substringAfter(':', "")
                    if (relative.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relative".substringBeforeLast('/')
                }.getOrDefault(rootPath)
            }
            FolderEntity(id = id, path = path, name = path.substringAfterLast('/'), songCount = group.size)
        }
        val genres = emptyList<GenreEntity>()
        db.replaceLibrary(songs = uniqueSongs, albums = albums, artists = artists, folders = folders, genres = genres)
        return uniqueSongs.size
    }

    private fun isAudioDocument(name: String, mimeType: String): Boolean {
        if (mimeType.startsWith("audio/", ignoreCase = true)) return true
        return name.substringAfterLast('.', "").lowercase() in setOf(
            "mp3", "m4a", "m4b", "aac", "flac", "wav", "ogg", "opus", "oga",
            "wma", "ape", "wv", "tta", "aif", "aiff", "dsf", "dff", "mid", "midi",
        )
    }

    private fun songFromDocument(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        modifiedMillis: Long,
        folderPath: String,
        relativePath: String,
    ): SongEntity {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            fun meta(key: Int): String = retriever.extractMetadata(key).orEmpty()
            val duration = meta(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0L
            val title = meta(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE).ifBlank { name.substringBeforeLast('.', name) }
            val artist = meta(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST).ifBlank { "<unknown>" }
            val album = meta(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM).ifBlank { "<unknown>" }
            val year = meta(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR).take(4).toIntOrNull() ?: 0
            val track = meta(android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).substringBefore('/').toIntOrNull() ?: 0
            val actualMime = meta(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE).ifBlank { mimeType }
            val id = stableId(uri.toString())
            val albumId = stableId("$artist\u0000$album")
            val artistId = stableId(artist)
            return SongEntity(
                id = id,
                uri = uri.toString(),
                title = title,
                trackNumber = track,
                discNumber = 1,
                year = year,
                durationMs = duration,
                dateAddedSec = modifiedMillis / 1000L,
                dateModifiedSec = modifiedMillis / 1000L,
                albumId = albumId,
                artistId = artistId,
                folderId = folderIdOf(folderPath),
                genreId = null,
                sizeBytes = sizeBytes,
                mimeType = actualMime,
                bitrate = meta(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE).toIntOrNull(),
                sampleRate = null,
                isSupported = isMimeSupported(actualMime),
                relativePath = relativePath,
                fileName = name,
                albumName = album,
                artistName = artist,
            )
        } catch (_: Exception) {
            // Some document providers/codecs reject MediaMetadataRetriever. The
            // SAF file is still a valid library item, so fall back to its filename.
            val title = name.substringBeforeLast('.', name)
            val artist = "<unknown>"
            val album = "<unknown>"
            val actualMime = mimeType.ifBlank {
                android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                    .orEmpty()
            }
            SongEntity(
                id = stableId(uri.toString()),
                uri = uri.toString(),
                title = title,
                trackNumber = 0,
                discNumber = 1,
                year = 0,
                durationMs = 0L,
                dateAddedSec = modifiedMillis / 1000L,
                dateModifiedSec = modifiedMillis / 1000L,
                albumId = stableId("$artist\u0000$album"),
                artistId = stableId(artist),
                folderId = folderIdOf(folderPath),
                genreId = null,
                sizeBytes = sizeBytes,
                mimeType = actualMime,
                bitrate = null,
                sampleRate = null,
                isSupported = isMimeSupported(actualMime),
                relativePath = relativePath,
                fileName = name,
                albumName = album,
                artistName = artist,
            )
        } finally {
            retriever.release()
        }
    }

    private fun stableId(value: String): Long {
        // Use a 64-bit digest-derived key to avoid collisions between thousands
        // of files; SAF-backed songs need stable IDs just like MediaStore songs.
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        val positive = java.nio.ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long and Long.MAX_VALUE
        return if (positive == 0L) Long.MIN_VALUE else -positive
    }

    /** SAF tree URI -> path for Android's primary shared-storage volume only. */
    private fun sharedStoragePathFromTreeUri(rawUri: String): String? {
        return try {
            val uri = Uri.parse(rawUri)
            if (uri.authority != "com.android.externalstorage.documents") return null
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            if (documentId.substringBefore(':') != "primary") return null
            val relative = documentId.substringAfter(':', "")
            val parts = relative.trim('/').split('/').filter { it.isNotBlank() }
            if (parts.any { it == "." || it == ".." }) return null
            if (parts.isEmpty()) EXTERNAL_STORAGE_PREFIX
            else EXTERNAL_STORAGE_PREFIX + "/" + parts.joinToString("/")
        } catch (_: Exception) {
            null
        }
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
