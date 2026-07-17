package com.resonate.player.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.db.PlaylistRow
import com.resonate.player.data.repo.PlaylistRepository
import com.resonate.player.data.repo.artworkUriFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<ImmutableList<PlaylistRow>> = repository.playlists()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Cover mosaic (§2.6): first four distinct album art uris per playlist. */
    val mosaics: StateFlow<ImmutableMap<Long, ImmutableList<String>>> = repository.songRefs()
        .map { refs ->
            refs.groupBy { it.playlistId }
                .mapValues { (_, items) ->
                    items.sortedBy { it.position }
                        .map { it.albumId }
                        .distinct()
                        .take(4)
                        .map { artworkUriFor(it) }
                        .toImmutableList()
                }
                .toImmutableMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentMapOf())

    fun create(name: String) = viewModelScope.launch { repository.create(name) }

    fun rename(id: Long, name: String) = viewModelScope.launch { repository.rename(id, name) }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    fun import(uri: android.net.Uri, name: String, onDone: (Int?) -> Unit) =
        viewModelScope.launch { onDone(repository.importM3u(uri, name)) }
}
