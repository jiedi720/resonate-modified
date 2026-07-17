package com.resonate.player.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.resonate.player.data.audio.ReplayGainResolver
import com.resonate.player.data.db.LongPositionDao
import com.resonate.player.data.db.LongPositionEntity
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.prefs.ReplayGainMode
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.data.prefs.UserPrefsStore
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wave 2 sound-quality effects, all service-side. One volume equation rules
 * them: player.volume = replayGain × crossfade × sleepFade.
 *
 * Crossfade here is a fade-through-transition (single-player architecture —
 * §5.1), not a two-player overlap. Replay gain is attenuation-only (no boost).
 */
class PlaybackEffectsController(
    private val context: Context,
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val prefsStore: UserPrefsStore,
    private val replayGainResolver: ReplayGainResolver,
    private val longPositionDao: LongPositionDao,
    private val songBrowseDao: SongBrowseDao,
) {
    private var prefs = UserPrefs()

    private var rgFactor = 1f
    private var crossfadeFactor = 1f
    var sleepFactor = 1f
        set(value) {
            field = value
            applyVolume()
        }

    private var fadeInStartedAt = 0L
    private var lastMediaId: String? = null
    private var skipInitialDeviceCallback = true

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // §2.7 resume on headphone/Bluetooth connect — via AudioDeviceCallback,
    // which needs no Bluetooth permission.
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            if (skipInitialDeviceCallback) {
                skipInitialDeviceCallback = false
                return
            }
            if (!prefs.resumeOnConnect) return
            val headphonesConnected = addedDevices.any {
                it.isSink && it.type in RESUME_DEVICE_TYPES
            }
            if (headphonesConnected && !player.isPlaying && player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    fun start() {
        scope.launch {
            prefsStore.prefs.collect { updated ->
                prefs = updated
                player.skipSilenceEnabled = updated.skipSilence
                applyReplayGain(player.currentMediaItem)
            }
        }
        scope.launch {
            while (isActive) {
                delay(250)
                updateCrossfade()
            }
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    fun release() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    fun onMediaItemTransition(current: MediaItem?, reason: Int) {
        fadeInStartedAt = SystemClock.elapsedRealtime()
        // A track that played to its end no longer needs a bookmark.
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            lastMediaId?.toLongOrNull()?.let { finished ->
                scope.launch(Dispatchers.IO) { longPositionDao.clear(finished) }
            }
        }
        lastMediaId = current?.mediaId
        applyReplayGain(current)
        restoreLongPosition(current)
    }

    /** Called from the position ticker and on pause. */
    fun saveCurrentLongPosition() {
        if (!prefs.longAudioMemory) return
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val duration = player.duration
        val position = player.currentPosition
        if (duration == C.TIME_UNSET || duration < LONG_AUDIO_MS) return
        scope.launch(Dispatchers.IO) {
            if (position < 10_000 || position > duration - 10_000) {
                longPositionDao.clear(songId)
            } else {
                longPositionDao.upsert(
                    LongPositionEntity(songId, position, System.currentTimeMillis())
                )
            }
        }
    }

    private fun restoreLongPosition(item: MediaItem?) {
        if (!prefs.longAudioMemory) return
        val songId = item?.mediaId?.toLongOrNull() ?: return
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                val durationMs = songBrowseDao.songById(songId)?.durationMs ?: return@withContext null
                if (durationMs < LONG_AUDIO_MS) null else longPositionDao.positionFor(songId)
            } ?: return@launch
            if (player.currentMediaItem?.mediaId == songId.toString()) {
                player.seekTo(saved)
            }
        }
    }

    private fun applyReplayGain(item: MediaItem?) {
        val songId = item?.mediaId?.toLongOrNull()
        if (songId == null || prefs.replayGainMode == ReplayGainMode.OFF) {
            rgFactor = 1f
            applyVolume()
            return
        }
        scope.launch {
            val gains = replayGainResolver.gainsFor(songId)
            val gainDb = when (prefs.replayGainMode) {
                ReplayGainMode.TRACK -> gains.trackGainDb ?: gains.albumGainDb
                ReplayGainMode.ALBUM -> gains.albumGainDb ?: gains.trackGainDb
                ReplayGainMode.OFF -> null
            }
            rgFactor = gainDb?.let { 10f.pow(it / 20f).coerceIn(0f, 1f) } ?: 1f
            applyVolume()
        }
    }

    private fun updateCrossfade() {
        val fadeMs = prefs.crossfadeSec * 1000L
        if (fadeMs <= 0L) {
            if (crossfadeFactor != 1f) {
                crossfadeFactor = 1f
                applyVolume()
            }
            return
        }
        if (!player.isPlaying) return

        var factor = 1f
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > fadeMs * 2) {
            val remaining = duration - player.currentPosition
            if (remaining < fadeMs) factor = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
        }
        val sinceTransition = SystemClock.elapsedRealtime() - fadeInStartedAt
        if (sinceTransition < fadeMs) {
            factor = minOf(factor, (sinceTransition.toFloat() / fadeMs).coerceIn(0f, 1f))
        }
        if (factor != crossfadeFactor) {
            crossfadeFactor = factor
            applyVolume()
        }
    }

    private fun applyVolume() {
        player.volume = (rgFactor * crossfadeFactor * sleepFactor).coerceIn(0f, 1f)
    }

    companion object {
        private const val LONG_AUDIO_MS = 20 * 60_000L

        private val RESUME_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}
