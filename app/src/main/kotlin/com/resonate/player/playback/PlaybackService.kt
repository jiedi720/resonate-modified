package com.resonate.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.glance.appwidget.updateAll
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.resonate.player.MainActivity
import com.resonate.player.data.db.PlayStatDao
import com.resonate.player.data.db.QueueDao
import com.resonate.player.data.prefs.UserPrefsStore
import com.resonate.player.data.repo.toDomain
import com.resonate.player.data.repo.toMediaItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * §5.1's one rule: UI ↔ MediaController ↔ this service ↔ ExoPlayer. The
 * notification, lock screen, Bluetooth, and (later) widget/Auto all hang off
 * this single session.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playStatDao: PlayStatDao

    @Inject
    lateinit var queueDao: QueueDao

    @Inject
    lateinit var prefsStore: UserPrefsStore

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionTicker: Job? = null
    private var sleepJob: Job? = null
    private var equalizerController: EqualizerController? = null

    override fun onCreate() {
        super.onCreate()

        // §6.6: local files don't need streaming buffers — 5s/10s saves ~15 MB.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5_000,
                /* maxBufferMs = */ 10_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000,
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(playerListener(player))

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(sessionCallback(player))
            .build()

        equalizerController = EqualizerController(prefsStore, scope).also { it.attach(player) }
        restoreSpeed(player)
        restoreQueue(player)
    }

    /** §2.4 speed sheet: persisted speed/pitch survives service restarts. */
    private fun restoreSpeed(player: Player) {
        scope.launch {
            val prefs = prefsStore.prefs.first()
            val speed = prefs.playbackSpeed.coerceIn(0.5f, 2f)
            val pitch = if (prefs.pitchCorrection) 1f else speed
            player.playbackParameters = PlaybackParameters(speed, pitch)
        }
    }

    private fun sessionCallback(player: Player) = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(CMD_SLEEP_TIMER, Bundle.EMPTY))
                        .build()
                )
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_SLEEP_TIMER) {
                setSleepTimer(player, args.getInt(ARG_MINUTES, SLEEP_CANCEL))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    /** §2.7 sleep timer: fade out over the last 20 seconds, then pause. */
    private fun setSleepTimer(player: Player, minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        player.volume = 1f
        when {
            minutes == SLEEP_CANCEL -> Unit

            minutes == SLEEP_END_OF_TRACK -> sleepJob = scope.launch {
                while (player.duration == C.TIME_UNSET || player.duration - player.currentPosition > 500) {
                    delay(500)
                }
                player.pause()
                player.volume = 1f
            }

            minutes > 0 -> sleepJob = scope.launch {
                val totalMs = minutes * 60_000L
                delay((totalMs - FADE_MS).coerceAtLeast(0))
                val steps = 20
                repeat(steps) { step ->
                    player.volume = 1f - (step + 1) / steps.toFloat()
                    delay(FADE_MS / steps)
                }
                player.pause()
                player.volume = 1f
            }
        }
    }

    /** §3 v1 #12: resume queue, track, and position across process death. */
    private fun restoreQueue(player: Player) {
        scope.launch {
            val songs = withContext(Dispatchers.IO) { queueDao.queueSongsOnce() }
            if (songs.isEmpty() || player.mediaItemCount > 0) return@launch
            val prefs = prefsStore.prefs.first()
            player.setMediaItems(
                songs.map { it.toDomain().toMediaItem() },
                prefs.queueIndex.coerceIn(0, songs.size - 1),
                prefs.queuePositionMs.coerceAtLeast(0),
            )
            player.prepare()
        }
    }

    private fun persistQueue(player: Player) {
        val ids = (0 until player.mediaItemCount).mapNotNull {
            player.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        scope.launch(Dispatchers.IO) { queueDao.replaceQueue(ids) }
    }

    private fun persistPosition(player: Player) {
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        scope.launch(Dispatchers.IO) {
            prefsStore.update { it.copy(queueIndex = index, queuePositionMs = position) }
        }
    }

    /** §2.8: push playback state into the Glance widget. */
    private fun updateWidget(player: Player) {
        val item = player.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString()
        val artist = item?.mediaMetadata?.artist?.toString()
        val artwork = item?.mediaMetadata?.artworkUri?.toString()
        val playing = player.isPlaying
        scope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(this@PlaybackService)
                val ids = manager.getGlanceIds(com.resonate.player.ui.widget.ResonateWidget::class.java)
                if (ids.isEmpty()) return@launch
                ids.forEach { id ->
                    androidx.glance.appwidget.state.updateAppWidgetState(this@PlaybackService, id) { prefs ->
                        if (title != null) {
                            prefs[com.resonate.player.ui.widget.ResonateWidget.KEY_TITLE] = title
                        }
                        prefs[com.resonate.player.ui.widget.ResonateWidget.KEY_ARTIST] = artist.orEmpty()
                        prefs[com.resonate.player.ui.widget.ResonateWidget.KEY_PLAYING] = playing
                        if (artwork != null) {
                            prefs[com.resonate.player.ui.widget.ResonateWidget.KEY_ARTWORK] = artwork
                        }
                    }
                }
                com.resonate.player.ui.widget.ResonateWidget().updateAll(this@PlaybackService)
            } catch (e: Exception) {
                Log.w(TAG, "Widget update failed", e)
            }
        }
    }

    private fun playerListener(player: Player) = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            persistPosition(player)
            updateWidget(player)
            val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
            scope.launch(Dispatchers.IO) {
                playStatDao.increment(songId, System.currentTimeMillis())
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                persistQueue(player)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateWidget(player)
            if (isPlaying) {
                positionTicker = scope.launch {
                    while (isActive) {
                        delay(10_000)
                        persistPosition(player)
                    }
                }
            } else {
                positionTicker?.cancel()
                positionTicker = null
                persistPosition(player)
            }
        }

        // §4: never crash the queue on a bad file — skip and keep going.
        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Playback error on ${player.currentMediaItem?.mediaId}", error)
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // §2.8: widget transport buttons arrive as start-service intents.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = mediaSession?.player
        when (intent?.action) {
            ACTION_WIDGET_PLAY_PAUSE -> player?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
            ACTION_WIDGET_NEXT -> player?.seekToNextMediaItem()
            ACTION_WIDGET_PREV -> player?.seekToPreviousMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // §6.7: release the player when the task is gone and nothing is playing.
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerController?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val FADE_MS = 20_000L

        const val CMD_SLEEP_TIMER = "com.resonate.player.SLEEP_TIMER"
        const val ARG_MINUTES = "minutes"
        const val SLEEP_CANCEL = -1
        const val SLEEP_END_OF_TRACK = -2

        const val ACTION_WIDGET_PLAY_PAUSE = "com.resonate.player.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.resonate.player.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.resonate.player.WIDGET_PREV"
    }
}
