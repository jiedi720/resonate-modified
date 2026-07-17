package com.resonate.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.repo.toDomain
import com.resonate.player.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    songBrowseDao: SongBrowseDao,
) : ViewModel() {

    private fun Flow<List<com.resonate.player.data.db.SongEntity>>.asSongs(): StateFlow<ImmutableList<Song>> =
        map { list -> list.map { it.toDomain() }.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val jumpBackIn: StateFlow<ImmutableList<Song>> = songBrowseDao.recentlyPlayed().asSongs()
    val recentlyAdded: StateFlow<ImmutableList<Song>> = songBrowseDao.recentlyAdded().asSongs()
    val mostPlayed: StateFlow<ImmutableList<Song>> = songBrowseDao.mostPlayed().asSongs()
}
