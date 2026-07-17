package com.resonate.player.data.audio

import android.util.Log
import com.resonate.player.data.db.FolderBrowseDao
import com.resonate.player.data.db.SongBrowseDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX

data class ReplayGains(
    val trackGainDb: Float?,
    val albumGainDb: Float?,
)

/** Parses "−6.54 dB"-style ReplayGain values. Pure function, unit-tested. */
object ReplayGainParser {
    fun parseGainDb(raw: String?): Float? = raw
        ?.trim()
        ?.removeSuffix("dB")
        ?.removeSuffix("DB")
        ?.removeSuffix("db")
        ?.trim()
        ?.replace('−', '-') // some taggers write a unicode minus
        ?.replace(",", ".")
        ?.toFloatOrNull()
        ?.takeIf { it.isFinite() && it > -60f && it < 60f }
}

/**
 * Wave 2 §2.7: replay gain (track/album/off). Tags are read once per song
 * from the file (TXXX frames on ID3, named fields on Vorbis/FLAC/M4A) and
 * LRU-cached. Mixed-source libraries finally play at one loudness.
 */
@Singleton
class ReplayGainResolver @Inject constructor(
    private val songBrowseDao: SongBrowseDao,
    private val folderBrowseDao: FolderBrowseDao,
) {
    private val cache = object : LinkedHashMap<Long, ReplayGains>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ReplayGains>): Boolean =
            size > CACHE_SIZE
    }

    suspend fun gainsFor(songId: Long): ReplayGains {
        synchronized(cache) { cache[songId]?.let { return it } }
        val result = withContext(Dispatchers.IO) { load(songId) }
        synchronized(cache) { cache[songId] = result }
        return result
    }

    private suspend fun load(songId: Long): ReplayGains {
        val song = songBrowseDao.songById(songId) ?: return NONE
        val folder = folderBrowseDao.folder(song.folderId).first() ?: return NONE
        val file = File(folder.path, song.fileName)
        return try {
            if (!file.canRead()) return NONE
            val tag = AudioFileIO.read(file).tag ?: return NONE
            ReplayGains(
                trackGainDb = ReplayGainParser.parseGainDb(customField(tag, TRACK_GAIN)),
                albumGainDb = ReplayGainParser.parseGainDb(customField(tag, ALBUM_GAIN)),
            )
        } catch (e: Exception) {
            Log.d(TAG, "No replay gain for ${file.name}: ${e.message}")
            NONE
        }
    }

    private fun customField(tag: Tag, name: String): String? {
        if (tag is AbstractID3v2Tag) {
            val frames = when (val frame = tag.getFrame("TXXX")) {
                is List<*> -> frame.filterIsInstance<AbstractID3v2Frame>()
                is AbstractID3v2Frame -> listOf(frame)
                else -> emptyList()
            }
            return frames
                .mapNotNull { it.body as? FrameBodyTXXX }
                .firstOrNull { it.description.equals(name, ignoreCase = true) }
                ?.text
        }
        // Vorbis/FLAC/M4A expose named fields directly.
        return try {
            tag.getFirst(name).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "ReplayGainResolver"
        private const val CACHE_SIZE = 32
        private const val TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
        private const val ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
        private val NONE = ReplayGains(null, null)
    }
}
