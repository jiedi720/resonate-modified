package com.resonate.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        FolderEntity::class,
        GenreEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayStatEntity::class,
        QueueItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ResonateDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun libraryAggregateDao(): LibraryAggregateDao
    abstract fun playStatDao(): PlayStatDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun songBrowseDao(): SongBrowseDao
    abstract fun albumBrowseDao(): AlbumBrowseDao
    abstract fun artistBrowseDao(): ArtistBrowseDao
    abstract fun folderBrowseDao(): FolderBrowseDao
    abstract fun genreBrowseDao(): GenreBrowseDao
}
