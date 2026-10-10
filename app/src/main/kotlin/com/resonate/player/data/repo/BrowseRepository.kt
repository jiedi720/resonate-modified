package com.resonate.player.data.repo

import android.content.ContentUris
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.resonate.player.data.db.AlbumBrowseDao
import com.resonate.player.data.db.AlbumEntity
import com.resonate.player.data.db.ArtistBrowseDao
import com.resonate.player.data.db.ArtistEntity
import com.resonate.player.data.db.FolderBrowseDao
import com.resonate.player.data.db.FolderEntity
import com.resonate.player.data.db.GenreBrowseDao
import com.resonate.player.data.db.GenreEntity
import com.resonate.player.data.db.LetterBucket
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.db.SongEntity
import com.resonate.player.data.prefs.SortField
import com.resonate.player.data.prefs.SortPref
import com.resonate.player.domain.model.Album
import com.resonate.player.domain.model.Artist
import com.resonate.player.domain.model.Folder
import com.resonate.player.domain.model.Genre
import com.resonate.player.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

fun artworkUriFor(albumId: Long): String =
    ContentUris.withAppendedId(ALBUM_ART_BASE, albumId).toString()

fun SongEntity.toDomain() = Song(
    id = id,
    uri = uri,
    title = title,
    artist = artistName,
    album = albumName,
    albumId = albumId,
    artistId = artistId,
    folderId = folderId,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    isSupported = isSupported,
    artworkUri = artworkUriFor(albumId),
    fileName = fileName,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    sizeBytes = sizeBytes,
)

fun AlbumEntity.toDomain() = Album(
    id = id,
    name = name,
    artistId = artistId,
    artistName = artistName,
    year = year,
    songCount = songCount,
    artworkUri = artworkUri ?: artworkUriFor(id),
)

fun ArtistEntity.toDomain() = Artist(id = id, name = name, albumCount = albumCount, songCount = songCount)
fun FolderEntity.toDomain() = Folder(id = id, path = path, name = name, songCount = songCount)
fun GenreEntity.toDomain() = Genre(id = id, name = name, songCount = songCount)

// §6.1: page size 60, prefetch 20; placeholders on so the fast-scroll rail
// can jump the full extent of the list without loading it.
private val PAGING_CONFIG = PagingConfig(
    pageSize = 60,
    prefetchDistance = 20,
    enablePlaceholders = true,
    initialLoadSize = 120,
)

@Singleton
class BrowseRepository @Inject constructor(
    private val songBrowseDao: SongBrowseDao,
    private val albumBrowseDao: AlbumBrowseDao,
    private val artistBrowseDao: ArtistBrowseDao,
    private val folderBrowseDao: FolderBrowseDao,
    private val genreBrowseDao: GenreBrowseDao,
) {

    fun songsPaged(sort: SortPref): Flow<PagingData<Song>> {
        val direction = if (sort.ascending) "ASC" else "DESC"
        val orderBy = when (sort.field) {
            SortField.ARTIST -> "s.artistName COLLATE NOCASE $direction, s.title COLLATE NOCASE ASC"
            SortField.ALBUM -> "s.albumName COLLATE NOCASE $direction, s.discNumber ASC, s.trackNumber ASC"
            SortField.DATE_ADDED -> "s.dateAddedSec $direction"
            SortField.DATE_MODIFIED -> "s.dateModifiedSec $direction"
            SortField.DURATION -> "s.durationMs $direction"
            SortField.PLAY_COUNT -> "COALESCE(p.playCount, 0) $direction, s.title COLLATE NOCASE ASC"
            else -> "s.title COLLATE NOCASE $direction"
        }
        val sql = "SELECT s.* FROM songs s LEFT JOIN play_stats p ON p.songId = s.id ORDER BY $orderBy"
        return Pager(PAGING_CONFIG) { songBrowseDao.songsPaged(SimpleSQLiteQuery(sql)) }
            .flow
            .map { paging -> paging.map { it.toDomain() } }
    }

    fun albumsPaged(sort: SortPref): Flow<PagingData<Album>> {
        val direction = if (sort.ascending) "ASC" else "DESC"
        val orderBy = when (sort.field) {
            SortField.ARTIST -> "artistName COLLATE NOCASE $direction, name COLLATE NOCASE ASC"
            SortField.YEAR -> "year $direction, name COLLATE NOCASE ASC"
            SortField.SONG_COUNT -> "songCount $direction, name COLLATE NOCASE ASC"
            else -> "name COLLATE NOCASE $direction"
        }
        return Pager(PAGING_CONFIG) {
            albumBrowseDao.albumsPaged(SimpleSQLiteQuery("SELECT * FROM albums ORDER BY $orderBy"))
        }.flow.map { paging -> paging.map { it.toDomain() } }
    }

    fun artistsPaged(sort: SortPref): Flow<PagingData<Artist>> {
        val direction = if (sort.ascending) "ASC" else "DESC"
        val orderBy = when (sort.field) {
            SortField.SONG_COUNT -> "songCount $direction, name COLLATE NOCASE ASC"
            else -> "name COLLATE NOCASE $direction"
        }
        return Pager(PAGING_CONFIG) {
            artistBrowseDao.artistsPaged(SimpleSQLiteQuery("SELECT * FROM artists ORDER BY $orderBy"))
        }.flow.map { paging -> paging.map { it.toDomain() } }
    }

    fun songLetterBuckets(): Flow<List<LetterBucket>> = songBrowseDao.titleLetterBuckets()
    fun albumLetterBuckets(): Flow<List<LetterBucket>> = albumBrowseDao.letterBuckets()
    fun artistLetterBuckets(): Flow<List<LetterBucket>> = artistBrowseDao.letterBuckets()

    fun allFolders(): Flow<List<Folder>> =
        folderBrowseDao.allFolders().map { list -> list.map { it.toDomain() } }

    fun allGenres(): Flow<List<Genre>> =
        genreBrowseDao.allGenres().map { list -> list.map { it.toDomain() } }

    fun album(id: Long): Flow<Album?> = albumBrowseDao.album(id).map { it?.toDomain() }
    fun artist(id: Long): Flow<Artist?> = artistBrowseDao.artist(id).map { it?.toDomain() }
    fun genre(id: Long): Flow<Genre?> = genreBrowseDao.genre(id).map { it?.toDomain() }
    fun folder(id: Long): Flow<Folder?> = folderBrowseDao.folder(id).map { it?.toDomain() }

    fun songsInAlbum(albumId: Long): Flow<List<Song>> =
        songBrowseDao.songsInAlbum(albumId).map { list -> list.map { it.toDomain() } }

    fun songsByArtist(artistId: Long): Flow<List<Song>> =
        songBrowseDao.songsByArtist(artistId).map { list -> list.map { it.toDomain() } }

    fun songsInGenre(genreId: Long): Flow<List<Song>> =
        songBrowseDao.songsInGenre(genreId).map { list -> list.map { it.toDomain() } }

    fun songsInFolder(folderId: Long): Flow<List<Song>> =
        songBrowseDao.songsInFolder(folderId).map { list -> list.map { it.toDomain() } }

    fun albumsByArtist(artistId: Long): Flow<List<Album>> =
        albumBrowseDao.albumsByArtist(artistId).map { list -> list.map { it.toDomain() } }
}
