package com.resonate.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.domain.model.Song
import com.resonate.player.domain.model.formatDuration
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.components.TrackRow
import com.resonate.player.ui.theme.ResonateTheme
import kotlinx.collections.immutable.ImmutableList

/** §2.1: jump back in, recently added, most played, shuffle. Nothing algorithmic. */
@Composable
fun HomeScreen(
    onShuffleAll: () -> Unit,
    onSongClick: (Song) -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val jumpBackIn by viewModel.jumpBackIn.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            Text(
                text = stringResource(R.string.tab_home),
                style = ResonateTheme.type.displaySm,
                color = ResonateTheme.colors.bone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        item(key = "shuffle", contentType = "shuffle") {
            Button(
                onClick = onShuffleAll,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResonateTheme.colors.accent,
                    contentColor = ResonateTheme.colors.onAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.home_shuffle_everything),
                    style = ResonateTheme.type.label,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (jumpBackIn.isEmpty() && recentlyAdded.isEmpty() && mostPlayed.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
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
        }

        if (jumpBackIn.isNotEmpty()) {
            item(key = "jump-back-in", contentType = "section") {
                SectionHeader(stringResource(R.string.home_jump_back_in))
                SongCardRow(songs = jumpBackIn, onSongClick = onSongClick)
            }
        }
        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently-added", contentType = "section") {
                SectionHeader(stringResource(R.string.home_recently_added))
                SongCardRow(songs = recentlyAdded, onSongClick = onSongClick)
            }
        }
        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played-header", contentType = "eyebrow") {
                SectionHeader(stringResource(R.string.home_most_played))
            }
            items(
                count = mostPlayed.size,
                key = { "most-played-${mostPlayed[it].id}" },
                contentType = { "track" },
            ) { index ->
                val song = mostPlayed[index]
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
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = ResonateTheme.type.caption,
        color = ResonateTheme.colors.muted,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SongCardRow(
    songs: ImmutableList<Song>,
    onSongClick: (Song) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = songs.size,
            key = { songs[it].id },
            contentType = { "card" },
        ) { index ->
            val song = songs[index]
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onSongClick(song) },
            ) {
                ArtworkImage(
                    uri = song.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    cornerRadius = 12.dp,
                )
                Text(
                    text = song.title,
                    style = ResonateTheme.type.title,
                    color = ResonateTheme.colors.bone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = song.artist,
                    style = ResonateTheme.type.body,
                    color = ResonateTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
