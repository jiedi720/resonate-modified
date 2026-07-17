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

    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS long_positions (" +
                    "songId INTEGER NOT NULL PRIMARY KEY, " +
                    "positionMs INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL)"
            )
        }
    }

    private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS play_events (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "songId INTEGER NOT NULL, " +
                    "playedAt INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_playedAt ON play_events(playedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_songId ON play_events(songId)")
        }
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ResonateDatabase =
        Room.databaseBuilder(context, ResonateDatabase::class.java, "resonate.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    @Provides
    fun longPositionDao(db: ResonateDatabase): com.resonate.player.data.db.LongPositionDao =
        db.longPositionDao()

    @Provides
    fun playEventDao(db: ResonateDatabase): com.resonate.player.data.db.PlayEventDao =
        db.playEventDao()
}
