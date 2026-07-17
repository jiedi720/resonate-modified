package com.resonate.player.data.lyrics

import android.util.Log
import com.resonate.player.data.db.FolderBrowseDao
import com.resonate.player.data.db.SongBrowseDao
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

/**
 * Lyrics, fully offline (§0 non-goals: no scraping): embedded tags first
 * (ID3 USLT, Vorbis LYRICS, M4A ©lyr via jaudiotagger), then a same-name
 * `.lrc` sidecar file. Either source may carry LRC timestamps → synced.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val songBrowseDao: SongBrowseDao,
    private val folderBrowseDao: FolderBrowseDao,
) {

    init {
        // jaudiotagger is chatty about imperfect tags; keep logcat quiet.
        Logger.getLogger("org.jaudiotagger").level = Level.SEVERE
    }

    private val cache = object : LinkedHashMap<Long, Lyrics?>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Lyrics?>): Boolean =
            size > CACHE_SIZE
    }

    suspend fun lyricsFor(songId: Long): Lyrics? {
        synchronized(cache) { if (cache.containsKey(songId)) return cache[songId] }
        val result = withContext(Dispatchers.IO) { load(songId) }
        synchronized(cache) { cache[songId] = result }
        return result
    }

    private suspend fun load(songId: Long): Lyrics? {
        val song = songBrowseDao.songById(songId) ?: return null
        val folderPath = folderBrowseDao.folder(song.folderId).first()?.path ?: return null
        val audioFile = File(folderPath, song.fileName)

        embeddedLyrics(audioFile)?.let { return it }

        val sidecar = File(folderPath, song.fileName.substringBeforeLast('.') + ".lrc")
        return sidecarLyrics(sidecar)
    }

    private fun embeddedLyrics(file: File): Lyrics? = try {
        if (!file.canRead()) {
            null
        } else {
            val tag = AudioFileIO.read(file).tag
            val raw = tag?.getFirst(FieldKey.LYRICS)
            if (raw.isNullOrBlank()) null else LrcParser.parse(raw)
        }
    } catch (e: Exception) {
        Log.d(TAG, "No embedded lyrics for ${file.name}: ${e.message}")
        null
    }

    private fun sidecarLyrics(file: File): Lyrics? = try {
        if (!file.canRead()) null else LrcParser.parse(file.readText())
    } catch (e: Exception) {
        // Scoped storage may deny non-audio reads on some devices — expected.
        Log.d(TAG, "No sidecar lyrics at ${file.name}: ${e.message}")
        null
    }

    companion object {
        private const val TAG = "LyricsRepository"
        private const val CACHE_SIZE = 8
    }
}
