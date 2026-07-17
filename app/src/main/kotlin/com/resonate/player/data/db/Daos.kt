package com.resonate.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SongIdUri(val id: Long, val uri: String)

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM songs")
    fun countFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Query("SELECT id, uri FROM songs")
    suspend fun idUriPairs(): List<SongIdUri>
}

@Dao
interface LibraryAggregateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAlbums()

    @Query("DELETE FROM artists")
    suspend fun deleteArtists()

    @Query("DELETE FROM folders")
    suspend fun deleteFolders()

    @Query("DELETE FROM genres")
    suspend fun deleteGenres()

    /** Carry chroma colors across rescans keyed by album id (step 6 fills them). */
    @Query("SELECT id, chromaPrimary, chromaSecondary, chromaOnColor FROM albums WHERE chromaPrimary IS NOT NULL")
    suspend fun chromaBackup(): List<ChromaBackup>
}

data class ChromaBackup(
    val id: Long,
    val chromaPrimary: Int?,
    val chromaSecondary: Int?,
    val chromaOnColor: Int?,
)

data class PlayStatBackup(
    val uri: String,
    val playCount: Int,
    val lastPlayedAt: Long,
    val isFavorite: Boolean,
)

@Dao
interface PlayStatDao {
    @Query(
        "SELECT s.uri AS uri, p.playCount AS playCount, p.lastPlayedAt AS lastPlayedAt, " +
            "p.isFavorite AS isFavorite FROM play_stats p INNER JOIN songs s ON s.id = p.songId"
    )
    suspend fun backupByUri(): List<PlayStatBackup>

    @Query("DELETE FROM play_stats")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<PlayStatEntity>)

    @Query("SELECT * FROM play_stats WHERE songId = :songId")
    suspend fun forSong(songId: Long): PlayStatEntity?

    @Query("UPDATE play_stats SET playCount = playCount + 1, lastPlayedAt = :now WHERE songId = :songId")
    suspend fun bump(songId: Long, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(stat: PlayStatEntity): Long

    // minSdk 26 ships a SQLite without UPSERT, so update-then-insert it is.
    @Transaction
    suspend fun increment(songId: Long, now: Long) {
        if (bump(songId, now) == 0) {
            insertIgnore(PlayStatEntity(songId = songId, playCount = 1, lastPlayedAt = now, isFavorite = false))
        }
    }

    @Query("SELECT isFavorite FROM play_stats WHERE songId = :songId")
    fun isFavoriteFlow(songId: Long): Flow<Boolean?>

    @Query("UPDATE play_stats SET isFavorite = CASE isFavorite WHEN 0 THEN 1 ELSE 0 END WHERE songId = :songId")
    suspend fun flipFavorite(songId: Long): Int

    @Transaction
    suspend fun toggleFavorite(songId: Long) {
        if (flipFavorite(songId) == 0) {
            insertIgnore(PlayStatEntity(songId = songId, playCount = 0, lastPlayedAt = 0, isFavorite = true))
        }
    }
}

data class PlaylistSongBackup(
    val playlistId: Long,
    val uri: String,
    val position: Int,
)

data class PlaylistRow(
    val id: Long,
    val name: String,
    val updatedAt: Long,
    val songCount: Int,
)

data class PlaylistSongRef(
    val playlistId: Long,
    val position: Int,
    val albumId: Long,
)

data class SongPathRow(
    val songId: Long,
    val fileName: String,
    val folderPath: String,
    val title: String,
    val artistName: String,
    val durationMs: Long,
)

@Dao
interface PlaylistDao {
    @Query(
        "SELECT ps.playlistId AS playlistId, s.uri AS uri, ps.position AS position " +
            "FROM playlist_songs ps INNER JOIN songs s ON s.id = ps.songId"
    )
    suspend fun songBackupByUri(): List<PlaylistSongBackup>

    @Query("DELETE FROM playlist_songs")
    suspend fun deleteAllPlaylistSongs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(items: List<PlaylistSongEntity>)

    // CRUD (§2.6)
    @Query(
        "SELECT p.id AS id, p.name AS name, p.updatedAt AS updatedAt, " +
            "COUNT(ps.songId) AS songCount FROM playlists p " +
            "LEFT JOIN playlist_songs ps ON ps.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.updatedAt DESC"
    )
    fun playlistRows(): Flow<List<PlaylistRow>>

    @Query(
        "SELECT ps.playlistId AS playlistId, ps.position AS position, s.albumId AS albumId " +
            "FROM playlist_songs ps INNER JOIN songs s ON s.id = ps.songId ORDER BY ps.position ASC"
    )
    fun songRefs(): Flow<List<PlaylistSongRef>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsOf(playlistId: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun playlist(id: Long): Flow<PlaylistEntity?>

    @Query(
        "SELECT s.* FROM playlist_songs ps INNER JOIN songs s ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC"
    )
    fun songsIn(playlistId: Long): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM playlist_songs ps INNER JOIN songs s ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC"
    )
    suspend fun songsInOnce(playlistId: Long): List<SongEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    /** Rewrites a playlist's membership in order — reorder/remove use this. */
    @Transaction
    suspend fun setSongs(playlistId: Long, songIds: List<Long>, now: Long) {
        deleteSongsOf(playlistId)
        insertPlaylistSongs(
            songIds.mapIndexed { index, songId ->
                PlaylistSongEntity(playlistId = playlistId, songId = songId, position = index)
            }
        )
        touch(playlistId, now)
    }

    @Transaction
    suspend fun appendSongs(playlistId: Long, songIds: List<Long>, now: Long) {
        var position = nextPosition(playlistId)
        insertPlaylistSongs(
            songIds.map { PlaylistSongEntity(playlistId = playlistId, songId = it, position = position++) }
        )
        touch(playlistId, now)
    }

    @Transaction
    suspend fun deletePlaylistFully(id: Long) {
        deleteSongsOf(id)
        deletePlaylist(id)
    }

    // M3U8 export needs real file paths (§2.6)
    @Query(
        "SELECT s.id AS songId, s.fileName AS fileName, f.path AS folderPath, " +
            "s.title AS title, s.artistName AS artistName, s.durationMs AS durationMs " +
            "FROM playlist_songs ps " +
            "INNER JOIN songs s ON s.id = ps.songId " +
            "INNER JOIN folders f ON f.id = s.folderId " +
            "WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC"
    )
    suspend fun songPathsIn(playlistId: Long): List<SongPathRow>

    @Query(
        "SELECT s.id AS songId, s.fileName AS fileName, f.path AS folderPath, " +
            "s.title AS title, s.artistName AS artistName, s.durationMs AS durationMs " +
            "FROM songs s INNER JOIN folders f ON f.id = s.folderId"
    )
    suspend fun allSongPaths(): List<SongPathRow>
}

data class QueueItemBackup(
    val position: Int,
    val uri: String,
)

@Dao
interface QueueDao {
    @Query(
        "SELECT q.position AS position, s.uri AS uri FROM queue_items q " +
            "INNER JOIN songs s ON s.id = q.songId"
    )
    suspend fun backupByUri(): List<QueueItemBackup>

    @Query("DELETE FROM queue_items")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Query(
        "SELECT s.* FROM queue_items q INNER JOIN songs s ON s.id = q.songId " +
            "ORDER BY q.position ASC"
    )
    suspend fun queueSongsOnce(): List<SongEntity>

    @Transaction
    suspend fun replaceQueue(songIds: List<Long>) {
        deleteAll()
        insertAll(songIds.mapIndexed { index, id -> QueueItemEntity(position = index, songId = id) })
    }
}
