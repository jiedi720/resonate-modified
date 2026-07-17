package com.resonate.player.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * §7's core guarantee: a rescan that reissues MediaStore IDs must never lose
 * play stats, favorites, playlist membership, or the queue. Reconcile by uri.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Robolectric 4.14 does not support SDK 36 yet
class LibraryReplacerTest {

    private lateinit var db: ResonateDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ResonateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun song(id: Long, uri: String) = SongEntity(
        id = id, uri = uri, title = "T$id", trackNumber = 1, discNumber = 1,
        year = 2024, durationMs = 200_000, dateAddedSec = 0, dateModifiedSec = 0,
        albumId = 1, artistId = 1, folderId = 1, genreId = null,
        sizeBytes = 1, mimeType = "audio/mpeg", bitrate = null, sampleRate = null,
        isSupported = true, relativePath = "Music", fileName = "t$id.mp3",
        albumName = "A", artistName = "R",
    )

    @Test
    fun `play stats survive a rescan that reissues ids`() = runTest {
        db.replaceLibrary(listOf(song(1, "content://media/1")), emptyList(), emptyList(), emptyList(), emptyList())
        db.playStatDao().insertAll(listOf(PlayStatEntity(songId = 1, playCount = 5, lastPlayedAt = 99, isFavorite = true)))

        // Rescan: same file, new MediaStore _ID.
        db.replaceLibrary(listOf(song(42, "content://media/1")), emptyList(), emptyList(), emptyList(), emptyList())

        val migrated = db.playStatDao().forSong(42)
        assertNotNull(migrated)
        assertEquals(5, migrated!!.playCount)
        assertEquals(true, migrated.isFavorite)
        assertNull(db.playStatDao().forSong(1))
    }

    @Test
    fun `queue and playlist membership survive id reissue`() = runTest {
        db.replaceLibrary(listOf(song(1, "u1"), song(2, "u2")), emptyList(), emptyList(), emptyList(), emptyList())
        db.playlistDao().insertPlaylistSongs(listOf(PlaylistSongEntity(playlistId = 7, songId = 2, position = 0)))
        db.queueDao().insertAll(listOf(QueueItemEntity(position = 0, songId = 1)))

        db.replaceLibrary(listOf(song(10, "u1"), song(20, "u2")), emptyList(), emptyList(), emptyList(), emptyList())

        val playlist = db.playlistDao().songBackupByUri()
        assertEquals(1, playlist.size)
        assertEquals("u2", playlist[0].uri)
        val queue = db.queueDao().backupByUri()
        assertEquals(1, queue.size)
        assertEquals("u1", queue[0].uri)
    }

    @Test
    fun `stats for files deleted from disk are dropped`() = runTest {
        db.replaceLibrary(listOf(song(1, "gone")), emptyList(), emptyList(), emptyList(), emptyList())
        db.playStatDao().insertAll(listOf(PlayStatEntity(songId = 1, playCount = 3, lastPlayedAt = 0, isFavorite = false)))

        db.replaceLibrary(listOf(song(2, "still-here")), emptyList(), emptyList(), emptyList(), emptyList())

        assertNull(db.playStatDao().forSong(1))
        assertNull(db.playStatDao().forSong(2))
    }
}
