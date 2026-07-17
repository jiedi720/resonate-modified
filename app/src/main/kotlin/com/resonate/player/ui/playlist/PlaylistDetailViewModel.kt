package com.resonate.player.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.resonate.player.data.db.PlaylistEntity
import com.resonate.player.data.repo.PlaylistRepository
import com.resonate.player.data.repo.toDomain
import com.resonate.player.domain.model.Song
import com.resonate.player.ui.navigation.PlaylistDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.toRoute<PlaylistDetailRoute>().playlistId

    val playlist: StateFlow<PlaylistEntity?> = repository.playlist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<ImmutableList<Song>> = repository.songsIn(playlistId)
        .map { list -> list.map { it.toDomain() }.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun rename(name: String) = viewModelScope.launch { repository.rename(playlistId, name) }

    fun removeAt(index: Int) = viewModelScope.launch {
        val ids = songs.value.map { it.id }.toMutableList()
        if (index in ids.indices) {
            ids.removeAt(index)
            repository.setSongs(playlistId, ids)
        }
    }

    fun move(from: Int, to: Int) = viewModelScope.launch {
        val ids = songs.value.map { it.id }.toMutableList()
        if (from in ids.indices && to in ids.indices) {
            val id = ids.removeAt(from)
            ids.add(to, id)
            repository.setSongs(playlistId, ids)
        }
    }

    fun export(target: android.net.Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
        onDone(repository.exportM3u(playlistId, target))
    }
}
