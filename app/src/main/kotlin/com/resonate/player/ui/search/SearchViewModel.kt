package com.resonate.player.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.prefs.UserPrefsStore
import com.resonate.player.data.repo.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchFilter { ALL, SONGS, ALBUMS, ARTISTS, FOLDERS }

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val prefsStore: UserPrefsStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    /** §2.3: debounced 120ms; the index lives only while this is collected. */
    val results: StateFlow<SearchRepository.Results?> = combine(
        _query.debounce(120),
        repository.index,
    ) { query, index ->
        if (query.isBlank()) null else repository.search(index, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentSearches: StateFlow<ImmutableList<String>> = prefsStore.prefs
        .map { it.recentSearches.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: SearchFilter) {
        _filter.value = value
    }

    /** Remember a query once the user acts on a result. */
    fun rememberSearch() {
        val current = _query.value.trim()
        if (current.length < 2) return
        viewModelScope.launch {
            prefsStore.update { prefs ->
                prefs.copy(
                    recentSearches = (listOf(current) + prefs.recentSearches.filter { it != current })
                        .take(8)
                )
            }
        }
    }

    fun clearRecent() = viewModelScope.launch {
        prefsStore.update { it.copy(recentSearches = emptyList()) }
    }
}
