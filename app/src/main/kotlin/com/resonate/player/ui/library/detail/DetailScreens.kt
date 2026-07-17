package com.resonate.player.ui.library.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.components.NumberedTrackRow
import com.resonate.player.ui.components.TrackRow
import com.resonate.player.ui.theme.ResonateTheme

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = ResonateTheme.colors.bone,
            )
        }
        Text(
            text = title,
            style = ResonateTheme.type.displaySm,
            color = ResonateTheme.colors.bone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun AlbumDetailScreen(onBack: () -> Unit, onSongClick: (Song) -> Unit) {
    val viewModel: AlbumDetailViewModel = hiltViewModel()
    val album by viewModel.album.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            DetailHeader(title = album?.name.orEmpty(), onBack = onBack)
        }
        item(key = "hero", contentType = "hero") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(
                    uri = album?.artworkUri,
                    contentDescription = stringResource(R.string.cd_artwork),
                    modifier = Modifier.size(120.dp),
                    cornerRadius = 12.dp,
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = album?.artistName.orEmpty(),
                        style = ResonateTheme.type.title,
                        color = ResonateTheme.colors.bone,
                    )
                    val meta = buildString {
                        album?.let {
                            if (it.year > 0) append("${it.year} · ")
                            append(stringResource(R.string.songs_count, it.songCount))
                        }
                    }
                    Text(
                        text = meta,
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
            NumberedTrackRow(
                number = song.trackNumber,
                title = song.title,
                subtitle = song.artist,
                duration = formatDuration(song.durationMs),
                supported = song.isSupported,
                onClick = { onSongClick(song) },
            )
        }
    }
}

@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSongClick: (Song) -> Unit,
) {
    val viewModel: ArtistDetailViewModel = hiltViewModel()
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            DetailHeader(title = artist?.name.orEmpty(), onBack = onBack)
        }
        if (albums.isNotEmpty()) {
            item(key = "albums", contentType = "albums") {
                Column {
                    Text(
                        text = stringResource(R.string.library_albums).uppercase(),
                        style = ResonateTheme.type.caption,
                        color = ResonateTheme.colors.muted,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            count = albums.size,
                            key = { albums[it].id },
                            contentType = { "album-card" },
                        ) { index ->
                            val album = albums[index]
                            Column(
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(bottom = 8.dp)
                                    .clickable { onAlbumClick(album.id) },
                            ) {
                                ArtworkImage(
                                    uri = album.artworkUri,
                                    contentDescription = album.name,
                                    modifier = Modifier.size(120.dp),
                                    cornerRadius = 12.dp,
                                )
                                Text(
                                    text = album.name,
                                    style = ResonateTheme.type.label,
                                    color = ResonateTheme.colors.bone,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "songs-header", contentType = "eyebrow") {
            Text(
                text = stringResource(R.string.library_songs).uppercase(),
                style = ResonateTheme.type.caption,
                color = ResonateTheme.colors.muted,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        items(
            count = songs.size,
            key = { songs[it].id },
            contentType = { "track" },
        ) { index ->
            val song = songs[index]
            TrackRow(
                title = song.title,
                subtitle = song.album,
                duration = formatDuration(song.durationMs),
                artworkUri = song.artworkUri,
                supported = song.isSupported,
                onClick = { onSongClick(song) },
            )
        }
    }
}

@Composable
fun FolderDetailScreen(onBack: () -> Unit, onSongClick: (Song) -> Unit) {
    val viewModel: FolderDetailViewModel = hiltViewModel()
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            DetailHeader(title = folder?.name.orEmpty(), onBack = onBack)
        }
        item(key = "path", contentType = "caption") {
            Text(
                text = folder?.path.orEmpty(),
                style = ResonateTheme.type.caption,
                color = ResonateTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
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

@Composable
fun GenreDetailScreen(onBack: () -> Unit, onSongClick: (Song) -> Unit) {
    val viewModel: GenreDetailViewModel = hiltViewModel()
    val genre by viewModel.genre.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            DetailHeader(title = genre?.name.orEmpty(), onBack = onBack)
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
