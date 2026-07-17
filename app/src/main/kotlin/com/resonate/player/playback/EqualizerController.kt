package com.resonate.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.data.prefs.UserPrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * §2.7: 5-band EQ + bass boost + virtualizer, driven entirely by prefs so the
 * settings screen never needs a line to the service. Effects attach to the
 * player's audio session and follow it if the session id changes.
 */
class EqualizerController(
    private val prefsStore: UserPrefsStore,
    private val scope: CoroutineScope,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var latestPrefs: UserPrefs? = null

    fun attach(player: ExoPlayer) {
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int,
            ) {
                rebuild(audioSessionId)
            }
        })
        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            rebuild(player.audioSessionId)
        }
        scope.launch {
            prefsStore.prefs.collect { prefs ->
                latestPrefs = prefs
                apply(prefs)
            }
        }
    }

    private fun rebuild(sessionId: Int) {
        release()
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            equalizer = Equalizer(0, sessionId)
            bassBoost = BassBoost(0, sessionId)
            virtualizer = Virtualizer(0, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Audio effects unavailable", e)
            release()
        }
        latestPrefs?.let(::apply)
    }

    private fun apply(prefs: UserPrefs) {
        try {
            equalizer?.let { eq ->
                eq.enabled = prefs.eqEnabled
                if (prefs.eqEnabled) {
                    if (prefs.eqPreset in 0 until eq.numberOfPresets) {
                        eq.usePreset(prefs.eqPreset.toShort())
                    } else {
                        prefs.eqBandLevels.forEachIndexed { band, level ->
                            if (band < eq.numberOfBands) {
                                eq.setBandLevel(band.toShort(), level.toShort())
                            }
                        }
                    }
                }
            }
            bassBoost?.let {
                it.enabled = prefs.eqEnabled && prefs.bassBoost > 0
                if (it.strengthSupported) it.setStrength(prefs.bassBoost.toShort())
            }
            virtualizer?.let {
                it.enabled = prefs.eqEnabled && prefs.virtualizer > 0
                if (it.strengthSupported) it.setStrength(prefs.virtualizer.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply audio effects", e)
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    companion object {
        private const val TAG = "EqualizerController"
    }
}
