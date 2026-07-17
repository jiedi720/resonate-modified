package com.resonate.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.mediastore.LibraryWatcher
import com.resonate.player.data.repo.LibraryRepository
import com.resonate.player.data.repo.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryScanViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val watcher: LibraryWatcher,
) : ViewModel() {

    val scanState: StateFlow<ScanState> = repository.scanState

    val songCount: StateFlow<Int> = repository.songCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Called when the audio permission is confirmed (fresh grant or app start). */
    fun onPermissionGranted() {
        watcher.start()
        viewModelScope.launch { repository.scanIfEmpty() }
    }

    fun rescan() {
        viewModelScope.launch { repository.rescan() }
    }
}
