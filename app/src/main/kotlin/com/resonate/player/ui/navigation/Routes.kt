package com.resonate.player.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LibraryRoute

@Serializable
data object SearchRoute

@Serializable
data object YouRoute

@Serializable
data class AlbumDetailRoute(val albumId: Long)

@Serializable
data class ArtistDetailRoute(val artistId: Long)

@Serializable
data class GenreDetailRoute(val genreId: Long)

@Serializable
data class PlaylistDetailRoute(val playlistId: Long)

@Serializable
data class FolderDetailRoute(val folderId: Long)

@Serializable
data object AppearanceSettingsRoute

@Serializable
data object LibrarySettingsRoute

@Serializable
data object EqualizerRoute

@Serializable
data object AboutRoute
