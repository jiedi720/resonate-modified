package com.resonate.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.resonate.player.data.db.PlayStatDao
import com.resonate.player.data.prefs.SortPref
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.data.prefs.UserPrefsStore
import com.resonate.player.data.repo.BrowseRepository
import com.resonate.player.domain.model.Album
import com.resonate.player.domain.model.Artist
import com.resonate.player.domain.model.FolderTree
import com.resonate.player.domain.model.Genre
import com.resonate.player.domain.model.Song
import com.resonate.player.ui.components.LetterIndexEntry
import com.resonate.player.ui.components.buildLetterIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryBrowseViewModel @Inject constructor(
    private val browse: BrowseRepository,
    private val prefsStore: UserPrefsStore,
    private val playStatDao: PlayStatDao,
) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val prefs: StateFlow<UserPrefs> = prefsStore.prefs.state(UserPrefs())

    // Songs
    val songSort: StateFlow<SortPref> =
        prefsStore.prefs.map { it.songSort }.distinctUntilChanged().state(SortPref())

    val songs: Flow<PagingData<Song>> =
        songSort.flatMapLatest { browse.songsPaged(it) }.cachedIn(viewModelScope)

    val songLetterIndex: StateFlow<ImmutableList<LetterIndexEntry>> =
        combine(browse.songLetterBuckets(), songSort) { buckets, sort ->
            if (sort.field == com.resonate.player.data.prefs.SortField.TITLE) {
                buildLetterIndex(buckets, sort.ascending).toImmutableList()
            } else {
                persistentListOf()
            }
        }.state(persistentListOf())

    val songCount: StateFlow<Int> =
        browse.songLetterBuckets().map { buckets -> buckets.sumOf { it.count } }.state(0)

    // Albums
    val albumSort: StateFlow<SortPref> =
        prefsStore.prefs.map { it.albumSort }.distinctUntilChanged()
            .state(SortPref(com.resonate.player.data.prefs.SortField.NAME, true))

    val albums: Flow<PagingData<Album>> =
        albumSort.flatMapLatest { browse.albumsPaged(it) }.cachedIn(viewModelScope)

    val albumLetterIndex: StateFlow<ImmutableList<LetterIndexEntry>> =
        combine(browse.albumLetterBuckets(), albumSort) { buckets, sort ->
            if (sort.field == com.resonate.player.data.prefs.SortField.NAME) {
                buildLetterIndex(buckets, sort.ascending).toImmutableList()
            } else {
                persistentListOf()
            }
        }.state(persistentListOf())

    // Artists
    val artistSort: StateFlow<SortPref> =
        prefsStore.prefs.map { it.artistSort }.distinctUntilChanged()
            .state(SortPref(com.resonate.player.data.prefs.SortField.NAME, true))

    val artists: Flow<PagingData<Artist>> =
        artistSort.flatMapLatest { browse.artistsPaged(it) }.cachedIn(viewModelScope)

    val artistLetterIndex: StateFlow<ImmutableList<LetterIndexEntry>> =
        combine(browse.artistLetterBuckets(), artistSort) { buckets, sort ->
            if (sort.field == com.resonate.player.data.prefs.SortField.NAME) {
                buildLetterIndex(buckets, sort.ascending).toImmutableList()
            } else {
                persistentListOf()
            }
        }.state(persistentListOf())

    // Folders — tree + current position
    val folderTree: StateFlow<FolderTree> =
        browse.allFolders().map { FolderTree.build(it) }.state(FolderTree.build(emptyList()))

    private val _currentFolderPath = MutableStateFlow<String?>(null)
    val currentFolderPath: StateFlow<String?> = _currentFolderPath.asStateFlow()

    val currentFolderSongs: StateFlow<ImmutableList<Song>> =
        combine(folderTree, _currentFolderPath) { tree, path ->
            path?.let { tree.node(it)?.folderId }
        }.distinctUntilChanged()
            .flatMapLatest { folderId ->
                if (folderId == null) {
                    kotlinx.coroutines.flow.flowOf(persistentListOf())
                } else {
                    browse.songsInFolder(folderId).map { it.toImmutableList() }
                }
            }.state(persistentListOf())

    suspend fun playCount(songId: Long): Int = playStatDao.forSong(songId)?.playCount ?: 0

    fun openFolder(path: String) {
        _currentFolderPath.value = path
    }

    fun navigateFolderUp(): Boolean {
        val current = _currentFolderPath.value ?: return false
        val tree = folderTree.value
        val parentPath = current.substringBeforeLast('/', missingDelimiterValue = "")
        _currentFolderPath.value =
            if (parentPath.isEmpty() || tree.node(parentPath) == null) null else parentPath
        return true
    }

    // Genres
    val genres: StateFlow<ImmutableList<Genre>> =
        browse.allGenres().map { it.toImmutableList() }.state(persistentListOf())

    // Persistence
    fun setTab(index: Int) = viewModelScope.launch {
        prefsStore.update { it.copy(lastLibraryTab = index) }
    }

    fun setSongSort(sort: SortPref) = viewModelScope.launch {
        prefsStore.update { it.copy(songSort = sort) }
    }

    fun setAlbumSort(sort: SortPref) = viewModelScope.launch {
        prefsStore.update { it.copy(albumSort = sort) }
    }

    fun setArtistSort(sort: SortPref) = viewModelScope.launch {
        prefsStore.update { it.copy(artistSort = sort) }
    }
}
