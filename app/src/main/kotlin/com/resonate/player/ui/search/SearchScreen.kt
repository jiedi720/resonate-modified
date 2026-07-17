package com.resonate.player.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.domain.model.Song
import com.resonate.player.domain.model.formatDuration
import com.resonate.player.ui.components.TrackRow
import com.resonate.player.ui.theme.ResonateTheme

/** §2.3: grouped results, filter chips, recent searches when idle. */
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onFolderClick: (Long) -> Unit,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val colors = ResonateTheme.colors
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.tab_search),
            style = ResonateTheme.type.displaySm,
            color = colors.bone,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            singleLine = true,
            shape = CircleShape,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = ResonateTheme.type.body,
                    color = colors.muted,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = colors.muted,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.hairline,
                unfocusedBorderColor = colors.hairline,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedTextColor = colors.bone,
                unfocusedTextColor = colors.bone,
                cursorColor = colors.accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        // Filter chips (§2.3)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filters = listOf(
                SearchFilter.SONGS to R.string.library_songs,
                SearchFilter.ALBUMS to R.string.library_albums,
                SearchFilter.ARTISTS to R.string.library_artists,
                SearchFilter.FOLDERS to R.string.library_folders,
            )
            items(count = filters.size, key = { filters[it].first.name }, contentType = { "chip" }) { i ->
                val (value, labelRes) = filters[i]
                FilterChip(
                    selected = filter == value,
                    onClick = {
                        viewModel.setFilter(if (filter == value) SearchFilter.ALL else value)
                    },
                    shape = CircleShape,
                    label = {
                        Text(
                            text = stringResource(labelRes),
                            style = ResonateTheme.type.label,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceRaised,
                        selectedLabelColor = colors.bone,
                        labelColor = colors.muted,
                    ),
                )
            }
        }

        val current = results
        when {
            query.isBlank() -> RecentSearches(
                recent = recent,
                onPick = viewModel::setQuery,
                onClear = viewModel::clearRecent,
            )

            current == null -> Unit

            current.isEmpty -> Text(
                text = stringResource(R.string.search_no_results, query),
                style = ResonateTheme.type.body,
                color = colors.muted,
                modifier = Modifier.padding(16.dp),
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (filter == SearchFilter.ALL || filter == SearchFilter.SONGS) {
                    if (current.songs.isNotEmpty()) {
                        item(key = "songs-header", contentType = "header") {
                            SectionHeader(stringResource(R.string.library_songs))
                        }
                        items(
                            count = current.songs.size,
                            key = { "song-${current.songs[it].id}" },
                            contentType = { "track" },
                        ) { i ->
                            val song = current.songs[i]
                            TrackRow(
                                title = song.title,
                                subtitle = song.artist,
                                duration = formatDuration(song.durationMs),
                                artworkUri = song.artworkUri,
                                supported = song.isSupported,
                                onClick = {
                                    viewModel.rememberSearch()
                                    onSongClick(song)
                                },
                            )
                        }
                    }
                }
                if (filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS) {
                    if (current.albums.isNotEmpty()) {
                        item(key = "albums-header", contentType = "header") {
                            SectionHeader(stringResource(R.string.library_albums))
                        }
                        items(
                            count = current.albums.size,
                            key = { "album-${current.albums[it].id}" },
                            contentType = { "album" },
                        ) { i ->
                            val album = current.albums[i]
                            TrackRow(
                                title = album.name,
                                subtitle = album.artistName,
                                duration = album.songCount.toString(),
                                artworkUri = album.artworkUri,
                                onClick = {
                                    viewModel.rememberSearch()
                                    onAlbumClick(album.id)
                                },
                            )
                        }
                    }
                }
                if (filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS) {
                    if (current.artists.isNotEmpty()) {
                        item(key = "artists-header", contentType = "header") {
                            SectionHeader(stringResource(R.string.library_artists))
                        }
                        items(
                            count = current.artists.size,
                            key = { "artist-${current.artists[it].id}" },
                            contentType = { "artist" },
                        ) { i ->
                            val artist = current.artists[i]
                            IconRow(
                                icon = { Icon(Icons.Filled.Person, null, tint = colors.muted) },
                                title = artist.name,
                                subtitle = stringResource(
                                    R.string.albums_and_songs, artist.albumCount, artist.songCount
                                ),
                                onClick = {
                                    viewModel.rememberSearch()
                                    onArtistClick(artist.id)
                                },
                            )
                        }
                    }
                }
                if (filter == SearchFilter.ALL || filter == SearchFilter.FOLDERS) {
                    if (current.folders.isNotEmpty()) {
                        item(key = "folders-header", contentType = "header") {
                            SectionHeader(stringResource(R.string.library_folders))
                        }
                        items(
                            count = current.folders.size,
                            key = { "folder-${current.folders[it].id}" },
                            contentType = { "folder" },
                        ) { i ->
                            val folder = current.folders[i]
                            IconRow(
                                icon = { Icon(Icons.Filled.Folder, null, tint = colors.muted) },
                                title = folder.name,
                                subtitle = stringResource(R.string.songs_count, folder.songCount),
                                onClick = {
                                    viewModel.rememberSearch()
                                    onFolderClick(folder.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = ResonateTheme.type.caption,
        color = ResonateTheme.colors.muted,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun IconRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = ResonateTheme.type.title,
                color = ResonateTheme.colors.bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = ResonateTheme.type.body,
                color = ResonateTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentSearches(
    recent: kotlinx.collections.immutable.ImmutableList<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) return
    LazyColumn {
        item(key = "recent-header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_recent).uppercase(),
                    style = ResonateTheme.type.caption,
                    color = ResonateTheme.colors.muted,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(R.string.search_clear_recent),
                        style = ResonateTheme.type.label,
                        color = ResonateTheme.colors.muted,
                    )
                }
            }
        }
        items(count = recent.size, key = { recent[it] }, contentType = { "recent" }) { i ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onPick(recent[i]) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = ResonateTheme.colors.muted,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = recent[i],
                    style = ResonateTheme.type.body,
                    color = ResonateTheme.colors.bone,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
