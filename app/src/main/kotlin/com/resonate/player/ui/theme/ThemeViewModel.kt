package com.resonate.player.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.data.prefs.UserPrefsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefsStore: UserPrefsStore,
) : ViewModel() {
    val prefs: StateFlow<UserPrefs> = prefsStore.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())

    fun update(transform: (UserPrefs) -> UserPrefs) = viewModelScope.launch {
        prefsStore.update(transform)
    }
}
