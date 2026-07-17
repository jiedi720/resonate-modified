package com.resonate.player.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

data class LetterBucket(val letter: String, val count: Int)

@Dao
interface SongBrowseDao {
    /** Sort is dynamic — Room can't parameterize ORDER BY, so the repo builds the SQL. */
    @RawQuery(observedEntities = [SongEntity::class, PlayStatEntity::class])
    fun songsPaged(query: SupportSQLiteQuery): PagingSource<Int, SongEntity>

    @Query(
        "SELECT UPPER(SUBSTR(title, 1, 1)) AS letter, COUNT(*) AS count FROM songs " +
            "GROUP BY letter ORDER BY letter COLLATE NOCASE ASC"
    )
    fun titleLetterBuckets(): Flow<List<LetterBucket>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
    fun songsInAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY title COLLATE NOCASE ASC")
    fun songsByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE genreId = :genreId ORDER BY title COLLATE NOCASE ASC")
    fun songsInGenre(genreId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folderId = :folderId ORDER BY fileName COLLATE NOCASE ASC")
    fun songsInFolder(folderId: Long): Flow<List<SongEntity>>

    // One-shot variants for queue building (§5.1).
    @RawQuery
    suspend fun songsListOnce(query: SupportSQLiteQuery): List<SongEntity>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
    suspend fun songsInAlbumOnce(albumId: Long): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsByArtistOnce(artistId: Long): List<SongEntity>

    @Query("SELECT * FROM songs WHERE genreId = :genreId ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsInGenreOnce(genreId: Long): List<SongEntity>

    @Query("SELECT * FROM songs WHERE folderId = :folderId ORDER BY fileName COLLATE NOCASE ASC")
    suspend fun songsInFolderOnce(folderId: Long): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isSupported != 0 ORDER BY RANDOM()")
    suspend fun allSongsShuffled(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun songById(id: Long): SongEntity?

    // §2.1 Home rows — nothing algorithmic, nothing networked.
    @Query(
        "SELECT s.* FROM songs s INNER JOIN play_stats p ON p.songId = s.id " +
            "WHERE p.lastPlayedAt > 0 ORDER BY p.lastPlayedAt DESC LIMIT 10"
    )
    fun recentlyPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAddedSec DESC LIMIT 10")
    fun recentlyAdded(): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM songs s INNER JOIN play_stats p ON p.songId = s.id " +
            "WHERE p.playCount > 0 ORDER BY p.playCount DESC LIMIT 10"
    )
    fun mostPlayed(): Flow<List<SongEntity>>

    /** Compact projection for the in-memory search index (§2.3). */
    @Query(
        "SELECT id, uri, title, artistName, albumName, fileName, albumId, artistId, " +
            "folderId, durationMs, isSupported FROM songs"
    )
    fun songSearchRows(): Flow<List<SongSearchRow>>
}

data class SongSearchRow(
    val id: Long,
    val uri: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val fileName: String,
    val albumId: Long,
    val artistId: Long,
    val folderId: Long,
    val durationMs: Long,
    val isSupported: Boolean,
)

@Dao
interface AlbumBrowseDao {
    @RawQuery(observedEntities = [AlbumEntity::class])
    fun albumsPaged(query: SupportSQLiteQuery): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albums")
    fun allAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun album(id: Long): Flow<AlbumEntity?>

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year DESC, name COLLATE NOCASE ASC")
    fun albumsByArtist(artistId: Long): Flow<List<AlbumEntity>>

    @Query(
        "SELECT UPPER(SUBSTR(name, 1, 1)) AS letter, COUNT(*) AS count FROM albums " +
            "GROUP BY letter ORDER BY letter COLLATE NOCASE ASC"
    )
    fun letterBuckets(): Flow<List<LetterBucket>>

    @Query("SELECT chromaPrimary, chromaSecondary, chromaOnColor FROM albums WHERE id = :albumId")
    suspend fun chromaFor(albumId: Long): AlbumChroma?

    @Query(
        "UPDATE albums SET chromaPrimary = :primary, chromaSecondary = :secondary, " +
            "chromaOnColor = :onColor WHERE id = :albumId"
    )
    suspend fun setChroma(albumId: Long, primary: Int, secondary: Int, onColor: Int)
}

data class AlbumChroma(
    val chromaPrimary: Int?,
    val chromaSecondary: Int?,
    val chromaOnColor: Int?,
)

@Dao
interface ArtistBrowseDao {
    @RawQuery(observedEntities = [ArtistEntity::class])
    fun artistsPaged(query: SupportSQLiteQuery): PagingSource<Int, ArtistEntity>

    @Query("SELECT * FROM artists")
    fun allArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun artist(id: Long): Flow<ArtistEntity?>

    @Query(
        "SELECT UPPER(SUBSTR(name, 1, 1)) AS letter, COUNT(*) AS count FROM artists " +
            "GROUP BY letter ORDER BY letter COLLATE NOCASE ASC"
    )
    fun letterBuckets(): Flow<List<LetterBucket>>
}

@Dao
interface FolderBrowseDao {
    @Query("SELECT * FROM folders ORDER BY path COLLATE NOCASE ASC")
    fun allFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun folder(id: Long): Flow<FolderEntity?>
}

@Dao
interface GenreBrowseDao {
    @Query("SELECT * FROM genres ORDER BY name COLLATE NOCASE ASC")
    fun allGenres(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genres WHERE id = :id")
    fun genre(id: Long): Flow<GenreEntity?>
}
