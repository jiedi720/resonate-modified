package com.resonate.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// §7 data model. Play stats/favorites live apart from songs so a rescan can
// wipe and rebuild the index without losing user history.

@Entity(
    tableName = "songs",
    indices = [Index("albumId"), Index("artistId"), Index("folderId"), Index("title")],
)
data class SongEntity(
    @PrimaryKey val id: Long, // MediaStore _ID
    val uri: String,
    val title: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val durationMs: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val albumId: Long,
    val artistId: Long,
    val folderId: Long,
    val genreId: Long?,
    val sizeBytes: Long,
    val mimeType: String,
    val bitrate: Int?,
    val sampleRate: Int?,
    val isSupported: Boolean,
    val relativePath: String,
    val fileName: String,
    val albumName: String,
    val artistName: String,
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val artistId: Long,
    val artistName: String,
    val year: Int,
    val songCount: Int,
    val artworkUri: String?,
    val chromaPrimary: Int?,
    val chromaSecondary: Int?,
    val chromaOnColor: Int?,
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: Long,
    val path: String,
    val name: String,
    val songCount: Int,
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val songCount: Int,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "position"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "play_stats")
data class PlayStatEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int,
    val lastPlayedAt: Long,
    val isFavorite: Boolean,
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val songId: Long,
)
