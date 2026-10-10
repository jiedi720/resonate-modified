package com.resonate.player.ui.library

import android.content.Intent
import android.provider.DocumentsContract
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.resonate.player.R
import com.resonate.player.data.prefs.SortField
import com.resonate.player.data.prefs.SortPref
import com.resonate.player.domain.model.Album
import com.resonate.player.domain.model.Artist
import com.resonate.player.domain.model.Song
import com.resonate.player.domain.model.formatDuration
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.components.FastScrollRail
import com.resonate.player.ui.components.TrackRow
import com.resonate.player.ui.components.TrackRowPlaceholder
import com.resonate.player.ui.playlist.PlaylistsTabContent
import com.resonate.player.ui.theme.ResonateTheme
import kotlinx.coroutines.launch

private val subTabs = listOf(
    R.string.library_songs,
    R.string.library_albums,
    R.string.library_artists,
    R.string.library_playlists,
    R.string.library_folders,
    R.string.library_genres,
)

@Composable
fun LibraryScreen(
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onGenreClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSongClick: (Song) -> Unit,
    onFolderSongClick: (Song) -> Unit,
    onAddSelection: (List<Song>) -> Unit,
) {
    val viewModel: LibraryBrowseViewModel = hiltViewModel()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(prefs.lastLibraryTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.tab_library),
            style = ResonateTheme.type.displaySm,
            color = ResonateTheme.colors.bone,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = subTabs.size,
                key = { subTabs[it] },
                contentType = { "chip" },
            ) { index ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        viewModel.setTab(index)
                    },
                    shape = CircleShape,
                    label = {
                        Text(
                            text = stringResource(subTabs[index]),
                            style = ResonateTheme.type.label,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ResonateTheme.colors.surfaceRaised,
                        selectedLabelColor = ResonateTheme.colors.bone,
                        labelColor = ResonateTheme.colors.muted,
                    ),
                )
            }
        }
        when (selectedTab) {
            0 -> SongsTab(viewModel, onSongClick, onAddSelection)
            1 -> AlbumsTab(viewModel, onAlbumClick)
            2 -> ArtistsTab(viewModel, onArtistClick)
            3 -> PlaylistsTabContent(onPlaylistClick = onPlaylistClick)
            4 -> FoldersTab(viewModel, onFolderSongClick)
            5 -> GenresTab(viewModel, onGenreClick)
        }
    }
}

// ---------- Songs ----------

@Composable
private fun SongsTab(
    viewModel: LibraryBrowseViewModel,
    onSongClick: (Song) -> Unit,
    onAddSelection: (List<Song>) -> Unit,
) {
    val songs: LazyPagingItems<Song> = viewModel.songs.collectAsLazyPagingItems()
    val letterIndex by viewModel.songLetterIndex.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val sort by viewModel.songSort.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scanViewModel: LibraryScanViewModel = hiltViewModel()
    var contextSong by remember { mutableStateOf<Song?>(null) }
    var contextPlayCount by remember { mutableStateOf<Int?>(null) }

    val moveFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { destinationTree ->
        val song = contextSong
        if (destinationTree != null && song != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(destinationTree, flags)
                val sourceUri = Uri.parse(song.uri)
                val sourceDocumentId = DocumentsContract.getDocumentId(sourceUri)
                val sourceParentId = sourceDocumentId.substringBeforeLast('/', missingDelimiterValue = "")
                require(sourceParentId.isNotBlank()) { "Cannot determine the source folder." }
                val sourceParent = DocumentsContract.buildDocumentUriUsingTree(sourceUri, sourceParentId)
                val destinationId = DocumentsContract.getTreeDocumentId(destinationTree)
                val destinationParent = DocumentsContract.buildDocumentUriUsingTree(destinationTree, destinationId)
                val moved = DocumentsContract.moveDocument(
                    context.contentResolver, sourceUri, sourceParent, destinationParent
                )
                if (moved != null) {
                    Toast.makeText(context, "Audio file moved.", Toast.LENGTH_SHORT).show()
                    contextSong = null
                    scanViewModel.rescan()
                } else {
                    Toast.makeText(context, "The file could not be moved.", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                Toast.makeText(context, "Move failed: ${error.message ?: "folder access unavailable"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Long press opens the track menu; multi-select remains available from that menu.
    val selectedSongs = remember { androidx.compose.runtime.mutableStateMapOf<Long, Song>() }
    val selectionMode = selectedSongs.isNotEmpty()
    BackHandler(enabled = selectionMode) { selectedSongs.clear() }

    fun toggle(song: Song) {
        if (selectedSongs.containsKey(song.id)) selectedSongs.remove(song.id)
        else selectedSongs[song.id] = song
    }

    if (songCount == 0) {
        EmptyLibrary()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectionMode) {
            SelectionBar(
                count = selectedSongs.size,
                onAdd = {
                    onAddSelection(selectedSongs.values.toList())
                    selectedSongs.clear()
                },
                onClose = { selectedSongs.clear() },
            )
        } else {
            CountAndSortBar(
                countText = stringResource(R.string.songs_count, songCount),
                sort = sort,
                options = songSortOptions,
                onSortChange = viewModel::setSongSort,
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(
                    count = songs.itemCount,
                    key = songs.itemKey { it.id },
                    contentType = songs.itemContentType { "track" },
                ) { index ->
                    val song = songs[index]
                    if (song != null) {
                        TrackRow(
                            title = song.title,
                            subtitle = song.artist,
                            duration = formatDuration(song.durationMs),
                            artworkUri = song.artworkUri,
                            trailingMetadata = (if (song.dateModifiedSec > 0L) song.dateModifiedSec else song.dateAddedSec)
                                .takeIf { it > 0L }
                                ?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it * 1000L)) },
                            supported = song.isSupported,
                            selected = selectedSongs.containsKey(song.id),
                            onClick = {
                                if (selectionMode) toggle(song) else onSongClick(song)
                            },
                            onLongClick = {
                                contextSong = song
                                contextPlayCount = null
                                scope.launch { contextPlayCount = viewModel.playCount(song.id) }
                            },
                        )
                    } else {
                        TrackRowPlaceholder()
                    }
                }
            }
            FastScrollRail(
                entries = letterIndex,
                onJump = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }

    val menuSong = contextSong
    if (menuSong != null) {
        AlertDialog(
            onDismissRequest = { contextSong = null },
            title = { Text(menuSong.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text(
                        text = "Played ${contextPlayCount?.toString() ?: "…"} times",
                        style = ResonateTheme.type.body,
                        color = ResonateTheme.colors.muted,
                    )
                    val artist = menuSong.artist.takeUnless { it.equals("<unknown>", ignoreCase = true) }
                    if (!artist.isNullOrBlank()) {
                        Text(artist, style = ResonateTheme.type.caption, color = ResonateTheme.colors.muted)
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        selectedSongs[menuSong.id] = menuSong
                        contextSong = null
                    }) { Text("Select for queue") }
                    TextButton(onClick = { moveFolderLauncher.launch(null) }) { Text("Move file") }
                }
            },
            dismissButton = {
                TextButton(onClick = { contextSong = null }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onAdd: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.selection_count, count),
            style = ResonateTheme.type.title,
            color = ResonateTheme.colors.accent,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(R.string.add_to_queue),
                tint = ResonateTheme.colors.bone,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_cancel),
                tint = ResonateTheme.colors.muted,
            )
        }
    }
}

// ---------- Albums ----------

@Composable
private fun AlbumsTab(viewModel: LibraryBrowseViewModel, onAlbumClick: (Long) -> Unit) {
    val albums: LazyPagingItems<Album> = viewModel.albums.collectAsLazyPagingItems()
    val letterIndex by viewModel.albumLetterIndex.collectAsStateWithLifecycle()
    val sort by viewModel.albumSort.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        CountAndSortBar(
            countText = stringResource(R.string.albums_count, albums.itemCount),
            sort = sort,
            options = albumSortOptions,
            onSortChange = viewModel::setAlbumSort,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(
                    count = albums.itemCount,
                    key = albums.itemKey { it.id },
                    contentType = albums.itemContentType { "album" },
                ) { index ->
                    val album = albums[index]
                    if (album != null) {
                        TrackRow(
                            title = album.name,
                            subtitle = if (album.year > 0) {
                                "${album.artistName} · ${album.year}"
                            } else {
                                album.artistName
                            },
                            duration = album.songCount.toString(),
                            artworkUri = album.artworkUri,
                            onClick = { onAlbumClick(album.id) },
                        )
                    } else {
                        TrackRowPlaceholder()
                    }
                }
            }
            FastScrollRail(
                entries = letterIndex,
                onJump = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

// ---------- Artists ----------

@Composable
private fun ArtistsTab(viewModel: LibraryBrowseViewModel, onArtistClick: (Long) -> Unit) {
    val artists: LazyPagingItems<Artist> = viewModel.artists.collectAsLazyPagingItems()
    val letterIndex by viewModel.artistLetterIndex.collectAsStateWithLifecycle()
    val sort by viewModel.artistSort.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        CountAndSortBar(
            countText = stringResource(R.string.artists_count, artists.itemCount),
            sort = sort,
            options = artistSortOptions,
            onSortChange = viewModel::setArtistSort,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(
                    count = artists.itemCount,
                    key = artists.itemKey { it.id },
                    contentType = artists.itemContentType { "artist" },
                ) { index ->
                    val artist = artists[index]
                    if (artist != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { onArtistClick(artist.id) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artist.name,
                                    style = ResonateTheme.type.title,
                                    color = ResonateTheme.colors.bone,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.albums_and_songs,
                                        artist.albumCount,
                                        artist.songCount,
                                    ),
                                    style = ResonateTheme.type.body,
                                    color = ResonateTheme.colors.muted,
                                )
                            }
                        }
                    } else {
                        TrackRowPlaceholder()
                    }
                }
            }
            FastScrollRail(
                entries = letterIndex,
                onJump = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

// ---------- Folders ----------

@Composable
private fun FoldersTab(viewModel: LibraryBrowseViewModel, onSongClick: (Song) -> Unit) {
    val tree by viewModel.folderTree.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentFolderPath.collectAsStateWithLifecycle()
    val songs by viewModel.currentFolderSongs.collectAsStateWithLifecycle()

    BackHandler(enabled = currentPath != null) {
        viewModel.navigateFolderUp()
    }

    val children = currentPath?.let { tree.childrenOf(it) } ?: tree.roots()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        currentPath?.let { path ->
            item(key = "breadcrumb", contentType = "breadcrumb") {
                Text(
                    text = path,
                    style = ResonateTheme.type.caption,
                    color = ResonateTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(
            count = children.size,
            key = { children[it].path },
            contentType = { "folder" },
        ) { index ->
            val node = children[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { viewModel.openFolder(node.path) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = ResonateTheme.colors.muted,
                    modifier = Modifier.size(32.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = node.name,
                        style = ResonateTheme.type.title,
                        color = ResonateTheme.colors.bone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.songs_count, node.totalCount),
                        style = ResonateTheme.type.body,
                        color = ResonateTheme.colors.muted,
                    )
                }
            }
        }
        items(
            count = songs.size,
            key = { songs[it].id },
            contentType = { "track" },
        ) { index ->
            val song = songs[index]
            TrackRow(
                title = song.title,
                subtitle = song.artist,
                duration = formatDuration(song.durationMs),
                artworkUri = song.artworkUri,
                supported = song.isSupported,
                onClick = { onSongClick(song) },
            )
        }
    }
}

// ---------- Genres ----------

@Composable
private fun GenresTab(viewModel: LibraryBrowseViewModel, onGenreClick: (Long) -> Unit) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()

    if (genres.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.empty_genres),
                style = ResonateTheme.type.body,
                color = ResonateTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = genres.size,
            key = { genres[it].id },
            contentType = { "genre" },
        ) { index ->
            val genre = genres[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onGenreClick(genre.id) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = genre.name,
                        style = ResonateTheme.type.title,
                        color = ResonateTheme.colors.bone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.songs_count, genre.songCount),
                        style = ResonateTheme.type.body,
                        color = ResonateTheme.colors.muted,
                    )
                }
            }
        }
    }
}

// ---------- Shared pieces ----------

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryMusic,
            contentDescription = null,
            tint = ResonateTheme.colors.muted,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(R.string.empty_library_title),
            style = ResonateTheme.type.displaySm,
            color = ResonateTheme.colors.bone,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.empty_library_body),
            style = ResonateTheme.type.body,
            color = ResonateTheme.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private data class SortOption(val field: SortField, val labelRes: Int)

private val songSortOptions = listOf(
    SortOption(SortField.TITLE, R.string.sort_title),
    SortOption(SortField.ARTIST, R.string.sort_artist),
    SortOption(SortField.ALBUM, R.string.sort_album),
    SortOption(SortField.DATE_ADDED, R.string.sort_date_added),
    SortOption(SortField.DURATION, R.string.sort_duration),
    SortOption(SortField.PLAY_COUNT, R.string.sort_play_count),
)

private val albumSortOptions = listOf(
    SortOption(SortField.NAME, R.string.sort_name),
    SortOption(SortField.ARTIST, R.string.sort_artist),
    SortOption(SortField.YEAR, R.string.sort_year),
    SortOption(SortField.SONG_COUNT, R.string.sort_song_count),
)

private val artistSortOptions = listOf(
    SortOption(SortField.NAME, R.string.sort_name),
    SortOption(SortField.SONG_COUNT, R.string.sort_song_count),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountAndSortBar(
    countText: String,
    sort: SortPref,
    options: List<SortOption>,
    onSortChange: (SortPref) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countText,
            style = ResonateTheme.type.caption,
            color = ResonateTheme.colors.muted,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { sheetOpen = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.cd_sort),
                tint = ResonateTheme.colors.muted,
            )
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = ResonateTheme.colors.surface,
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                options.forEach { option ->
                    val selected = sort.field == option.field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                onSortChange(
                                    if (selected) {
                                        sort.copy(ascending = !sort.ascending)
                                    } else {
                                        SortPref(option.field, true)
                                    }
                                )
                            }
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(option.labelRes),
                            style = ResonateTheme.type.title,
                            color = if (selected) ResonateTheme.colors.accent else ResonateTheme.colors.bone,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Text(
                                text = stringResource(
                                    if (sort.ascending) R.string.sort_ascending else R.string.sort_descending
                                ),
                                style = ResonateTheme.type.label,
                                color = ResonateTheme.colors.muted,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = ResonateTheme.colors.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}
