package com.resonate.player.ui.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.resonate.player.data.repo.BrowseRepository
import com.resonate.player.domain.model.Album
import com.resonate.player.domain.model.Artist
import com.resonate.player.domain.model.Genre
import com.resonate.player.domain.model.Song
import com.resonate.player.ui.navigation.AlbumDetailRoute
import com.resonate.player.ui.navigation.ArtistDetailRoute
import com.resonate.player.ui.navigation.GenreDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    browse: BrowseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val albumId = savedStateHandle.toRoute<AlbumDetailRoute>().albumId

    val album: StateFlow<Album?> = browse.album(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<ImmutableList<Song>> = browse.songsInAlbum(albumId)
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    browse: BrowseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.toRoute<ArtistDetailRoute>().artistId

    val artist: StateFlow<Artist?> = browse.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val albums: StateFlow<ImmutableList<Album>> = browse.albumsByArtist(artistId)
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val songs: StateFlow<ImmutableList<Song>> = browse.songsByArtist(artistId)
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}

@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    browse: BrowseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val genreId = savedStateHandle.toRoute<GenreDetailRoute>().genreId

    val genre: StateFlow<Genre?> = browse.genre(genreId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<ImmutableList<Song>> = browse.songsInGenre(genreId)
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    browse: BrowseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val folderId = savedStateHandle.toRoute<com.resonate.player.ui.navigation.FolderDetailRoute>().folderId

    val folder: StateFlow<com.resonate.player.domain.model.Folder?> = browse.folder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<ImmutableList<Song>> = browse.songsInFolder(folderId)
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}
