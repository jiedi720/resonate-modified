package com.resonate.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.resonate.player.data.db.PlayStatDao
import com.resonate.player.data.prefs.SortPref
import com.resonate.player.data.prefs.UserPrefsStore
import com.resonate.player.data.repo.PlaybackRepository
import com.resonate.player.playback.PlayerConnection
import com.resonate.player.ui.theme.ChromaColors
import com.resonate.player.ui.theme.ChromaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val connection: PlayerConnection,
    private val playStatDao: PlayStatDao,
    private val chromaEngine: ChromaEngine,
    private val playlistRepository: com.resonate.player.data.repo.PlaylistRepository,
    private val prefsStore: UserPrefsStore,
) : ViewModel() {

    val nowPlaying: StateFlow<PlayerConnection.NowPlaying?> = connection.nowPlaying
    val isPlaying: StateFlow<Boolean> = connection.isPlaying
    val shuffleEnabled: StateFlow<Boolean> = connection.shuffleEnabled
    val repeatMode: StateFlow<Int> = connection.repeatMode
    val durationMs: StateFlow<Long> = connection.durationMs
    val queue = connection.queue
    val queueIndex: StateFlow<Int> = connection.currentIndex

    fun moveQueueItem(from: Int, to: Int) = connection.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = connection.removeQueueItem(index)
    fun playQueueItem(index: Int) = connection.playQueueItem(index)
    fun clearQueue() = connection.clearQueue()

    /** Polled position for seek bars; only hot while collected. */
    val positionMs: Flow<Long> = flow {
        while (true) {
            emit(connection.currentPositionMs)
            delay(500)
        }
    }

    private val songSort: StateFlow<SortPref> = prefsStore.prefs
        .map { it.songSort }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortPref())

    fun playFromAllSongs(startSongId: Long) = viewModelScope.launch {
        playbackRepository.playAllSongs(songSort.value, startSongId)
    }

    fun playAlbum(albumId: Long, startSongId: Long?) = viewModelScope.launch {
        playbackRepository.playAlbum(albumId, startSongId)
    }

    fun playArtist(artistId: Long, startSongId: Long?) = viewModelScope.launch {
        playbackRepository.playArtist(artistId, startSongId)
    }

    fun playGenre(genreId: Long, startSongId: Long?) = viewModelScope.launch {
        playbackRepository.playGenre(genreId, startSongId)
    }

    fun playFolder(folderId: Long, startSongId: Long?) = viewModelScope.launch {
        playbackRepository.playFolder(folderId, startSongId)
    }

    fun shuffleAll() = viewModelScope.launch {
        playbackRepository.shuffleAll()
    }

    fun playPlaylist(playlistId: Long, startIndex: Int) = viewModelScope.launch {
        playbackRepository.playPlaylist(playlistId, startIndex)
    }

    fun addToQueue(song: com.resonate.player.domain.model.Song) = viewModelScope.launch {
        playbackRepository.addToQueue(song)
    }

    fun saveQueueAsPlaylist(name: String) = viewModelScope.launch {
        val ids = connection.queue.value.map { it.songId }.filter { it > 0 }
        if (ids.isNotEmpty()) playlistRepository.create(name, ids)
    }

    // §2.4 playback speed — applied live and persisted
    val playbackSpeed: StateFlow<Float> = prefsStore.prefs
        .map { it.playbackSpeed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    val pitchCorrection: StateFlow<Boolean> = prefsStore.prefs
        .map { it.pitchCorrection }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setSpeed(speed: Float, pitchCorrection: Boolean) {
        connection.setPlaybackSpeed(speed, pitchCorrection)
        viewModelScope.launch {
            prefsStore.update { it.copy(playbackSpeed = speed, pitchCorrection = pitchCorrection) }
        }
    }

    // §2.7 sleep timer — service-side; sleepEndsAt is a local display hint
    private val _sleepEndsAt = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val sleepEndsAt: StateFlow<Long?> = _sleepEndsAt

    fun setSleepTimer(minutes: Int) {
        connection.setSleepTimer(minutes)
        _sleepEndsAt.value = when {
            minutes > 0 -> System.currentTimeMillis() + minutes * 60_000L
            minutes == com.resonate.player.playback.PlaybackService.SLEEP_END_OF_TRACK -> -1L
            else -> null
        }
    }

    fun playPause() = connection.playPause()
    fun next() = connection.next()
    fun previous() = connection.previous()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun toggleShuffle() = connection.toggleShuffle()
    fun cycleRepeatMode() = connection.cycleRepeatMode()

    val isRepeatOne: StateFlow<Boolean> = connection.repeatMode
        .map { it == Player.REPEAT_MODE_ONE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isFavorite: StateFlow<Boolean> = connection.nowPlaying
        .flatMapLatest { playing ->
            if (playing == null) flowOf(false)
            else playStatDao.isFavoriteFlow(playing.songId).map { it == true }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavorite() = viewModelScope.launch {
        connection.nowPlaying.value?.let { playStatDao.toggleFavorite(it.songId) }
    }

    /** §1.1: the room changes color with the track. Null = fallback accents. */
    val chroma: StateFlow<ChromaColors?> = combine(
        connection.nowPlaying,
        prefsStore.prefs,
    ) { playing, prefs ->
        if (!prefs.chromaEnabled) null else playing?.albumId?.let { it to playing.artworkUri }
    }
        .distinctUntilChanged()
        .mapLatest { key ->
            key?.let { (albumId, artworkUri) -> chromaEngine.chromaForAlbum(albumId, artworkUri) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
