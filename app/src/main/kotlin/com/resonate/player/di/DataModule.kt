package com.resonate.player.di

import android.content.Context
import androidx.room.Room
import com.resonate.player.data.db.AlbumBrowseDao
import com.resonate.player.data.db.ArtistBrowseDao
import com.resonate.player.data.db.FolderBrowseDao
import com.resonate.player.data.db.GenreBrowseDao
import com.resonate.player.data.db.LibraryAggregateDao
import com.resonate.player.data.db.PlayStatDao
import com.resonate.player.data.db.PlaylistDao
import com.resonate.player.data.db.QueueDao
import com.resonate.player.data.db.ResonateDatabase
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.db.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ResonateDatabase =
        Room.databaseBuilder(context, ResonateDatabase::class.java, "resonate.db")
            .build()

    @Provides
    fun songDao(db: ResonateDatabase): SongDao = db.songDao()

    @Provides
    fun libraryAggregateDao(db: ResonateDatabase): LibraryAggregateDao = db.libraryAggregateDao()

    @Provides
    fun playStatDao(db: ResonateDatabase): PlayStatDao = db.playStatDao()

    @Provides
    fun playlistDao(db: ResonateDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun queueDao(db: ResonateDatabase): QueueDao = db.queueDao()

    @Provides
    fun songBrowseDao(db: ResonateDatabase): SongBrowseDao = db.songBrowseDao()

    @Provides
    fun albumBrowseDao(db: ResonateDatabase): AlbumBrowseDao = db.albumBrowseDao()

    @Provides
    fun artistBrowseDao(db: ResonateDatabase): ArtistBrowseDao = db.artistBrowseDao()

    @Provides
    fun folderBrowseDao(db: ResonateDatabase): FolderBrowseDao = db.folderBrowseDao()

    @Provides
    fun genreBrowseDao(db: ResonateDatabase): GenreBrowseDao = db.genreBrowseDao()
}
