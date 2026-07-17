package com.resonate.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The UI's only door to playback (§5.1). Wraps a MediaController; never
 * exposes ExoPlayer. All state the UI needs is mirrored into StateFlows.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class NowPlaying(
        val songId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val albumId: Long?,
        val artworkUri: String?,
    )

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    data class QueueEntry(
        val songId: Long,
        val title: String,
        val artist: String,
        val artworkUri: String?,
    )

    private val _queue = MutableStateFlow<ImmutableList<QueueEntry>>(persistentListOf())
    val queue: StateFlow<ImmutableList<QueueEntry>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var controller: MediaController? = null
    private val controllerReady = CompletableDeferred<MediaController>()

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val ready = future.get()
                controller = ready
                ready.addListener(listener)
                syncFromController(ready)
                controllerReady.complete(ready)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            controller?.let(::syncFromController)
        }

        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            controller?.let(::syncFromController)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { _durationMs.value = it.duration.coerceAtLeast(0) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            controller?.let(::syncQueue)
        }
    }

    private fun syncQueue(controller: MediaController) {
        _queue.value = (0 until controller.mediaItemCount).map { index ->
            val item = controller.getMediaItemAt(index)
            QueueEntry(
                songId = item.mediaId.toLongOrNull() ?: -1L,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
            )
        }.toImmutableList()
        _currentIndex.value = controller.currentMediaItemIndex
    }

    private fun syncFromController(controller: MediaController) {
        syncQueue(controller)
        val item = controller.currentMediaItem
        _nowPlaying.value = item?.let {
            NowPlaying(
                songId = it.mediaId.toLongOrNull() ?: -1L,
                title = it.mediaMetadata.title?.toString().orEmpty(),
                artist = it.mediaMetadata.artist?.toString().orEmpty(),
                album = it.mediaMetadata.albumTitle?.toString().orEmpty(),
                albumId = it.mediaMetadata.extras?.getLong("albumId", -1L)?.takeIf { id -> id > 0 },
                artworkUri = it.mediaMetadata.artworkUri?.toString(),
            )
        }
        _isPlaying.value = controller.isPlaying
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _durationMs.value = controller.duration.coerceAtLeast(0)
    }

    internal suspend fun awaitController(): MediaController = controllerReady.await()

    val currentPositionMs: Long
        get() = controller?.currentPosition ?: 0L

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.let {
            // Standard transport behavior: restart if >3s in, else go back.
            if (it.currentPosition > 3_000 || !it.hasPreviousMediaItem()) it.seekTo(0)
            else it.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /** §2.4: 0.5×–2.0×; correction keeps pitch at 1.0 while speed changes. */
    fun setPlaybackSpeed(speed: Float, pitchCorrection: Boolean) {
        controller?.playbackParameters = androidx.media3.common.PlaybackParameters(
            speed.coerceIn(0.5f, 2f),
            if (pitchCorrection) 1f else speed.coerceIn(0.5f, 2f),
        )
    }

    /** §2.7 sleep timer: minutes, or the SLEEP_* sentinels in PlaybackService. */
    fun setSleepTimer(minutes: Int) {
        controller?.sendCustomCommand(
            androidx.media3.session.SessionCommand(PlaybackService.CMD_SLEEP_TIMER, android.os.Bundle.EMPTY),
            android.os.Bundle().apply { putInt(PlaybackService.ARG_MINUTES, minutes) },
        )
    }

    // Queue ops (§2.5)
    fun moveQueueItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) {
        controller?.removeMediaItem(index)
    }

    fun playQueueItem(index: Int) {
        controller?.let {
            it.seekTo(index, 0)
            it.play()
        }
    }

    fun clearQueue() {
        controller?.let {
            it.clearMediaItems()
            it.stop()
        }
    }
}
