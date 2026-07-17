package com.resonate.player.data.repo

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.sqlite.db.SimpleSQLiteQuery
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.db.SongEntity
import com.resonate.player.data.prefs.SortField
import com.resonate.player.data.prefs.SortPref
import com.resonate.player.domain.model.Song
import com.resonate.player.playback.PlayerConnection
import javax.inject.Inject
import javax.inject.Singleton

const val EXTRA_ALBUM_ID = "albumId"

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let(android.net.Uri::parse))
            .setTrackNumber(trackNumber)
            .setDiscNumber(discNumber)
            .setExtras(android.os.Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) })
            .build()
    )
    .build()

fun songOrderBy(sort: SortPref): String {
    val direction = if (sort.ascending) "ASC" else "DESC"
    return when (sort.field) {
        SortField.ARTIST -> "s.artistName COLLATE NOCASE $direction, s.title COLLATE NOCASE ASC"
        SortField.ALBUM -> "s.albumName COLLATE NOCASE $direction, s.discNumber ASC, s.trackNumber ASC"
        SortField.DATE_ADDED -> "s.dateAddedSec $direction"
        SortField.DURATION -> "s.durationMs $direction"
        SortField.PLAY_COUNT -> "COALESCE(p.playCount, 0) $direction, s.title COLLATE NOCASE ASC"
        else -> "s.title COLLATE NOCASE $direction"
    }
}

/** Builds queues from the index and hands them to the session (§5.1). */
@Singleton
class PlaybackRepository @Inject constructor(
    private val songBrowseDao: SongBrowseDao,
    private val playlistDao: com.resonate.player.data.db.PlaylistDao,
    private val connection: PlayerConnection,
) {

    private suspend fun play(songs: List<SongEntity>, startSongId: Long?) {
        // §4: unsupported files never enter the queue — they're dimmed in UI.
        val playable = songs.filter { it.isSupported }
        if (playable.isEmpty()) return
        val startIndex = startSongId?.let { id -> playable.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: 0
        val controller = connection.awaitController()
        controller.setMediaItems(playable.map { it.toDomain().toMediaItem() }, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    suspend fun playAllSongs(sort: SortPref, startSongId: Long) {
        val sql = "SELECT s.* FROM songs s LEFT JOIN play_stats p ON p.songId = s.id " +
            "ORDER BY ${songOrderBy(sort)}"
        play(songBrowseDao.songsListOnce(SimpleSQLiteQuery(sql)), startSongId)
    }

    suspend fun playAlbum(albumId: Long, startSongId: Long?) {
        play(songBrowseDao.songsInAlbumOnce(albumId), startSongId)
    }

    suspend fun playArtist(artistId: Long, startSongId: Long?) {
        play(songBrowseDao.songsByArtistOnce(artistId), startSongId)
    }

    suspend fun playGenre(genreId: Long, startSongId: Long?) {
        play(songBrowseDao.songsInGenreOnce(genreId), startSongId)
    }

    suspend fun playFolder(folderId: Long, startSongId: Long?) {
        play(songBrowseDao.songsInFolderOnce(folderId), startSongId)
    }

    suspend fun shuffleAll() {
        play(songBrowseDao.allSongsShuffled(), null)
    }

    suspend fun playPlaylist(playlistId: Long, startIndex: Int) {
        val songs = playlistDao.songsInOnce(playlistId)
        play(songs, songs.getOrNull(startIndex)?.id)
    }

    suspend fun addToQueue(song: Song) {
        val controller = connection.awaitController()
        controller.addMediaItem(song.toMediaItem())
        if (controller.mediaItemCount == 1) {
            controller.prepare()
        }
    }
}
