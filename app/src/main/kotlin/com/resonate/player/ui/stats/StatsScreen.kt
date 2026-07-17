package com.resonate.player.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.data.db.ArtistPlayCount
import com.resonate.player.data.db.PlayEventDao
import com.resonate.player.data.db.SongPlayCount
import com.resonate.player.data.repo.artworkUriFor
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.theme.ResonateTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private fun startOfMonth(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Wave 3: "your sound" — computed locally, from your own play events. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    playEventDao: PlayEventDao,
) : ViewModel() {
    private val since = startOfMonth()

    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val playsThisMonth: StateFlow<Int> = playEventDao.playCountSince(since).state(0)

    val minutesThisMonth: StateFlow<Long> =
        playEventDao.listeningMsSince(since).map { it / 60_000 }.state(0)

    val topArtists: StateFlow<ImmutableList<ArtistPlayCount>> =
        playEventDao.topArtistsSince(since, 5).map { it.toImmutableList() }.state(persistentListOf())

    val topSongs: StateFlow<ImmutableList<SongPlayCount>> =
        playEventDao.topSongsSince(since, 5).map { it.toImmutableList() }.state(persistentListOf())
}

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val viewModel: StatsViewModel = hiltViewModel()
    val colors = ResonateTheme.colors
    val plays by viewModel.playsThisMonth.collectAsStateWithLifecycle()
    val minutes by viewModel.minutesThisMonth.collectAsStateWithLifecycle()
    val topArtists by viewModel.topArtists.collectAsStateWithLifecycle()
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
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
                        tint = colors.bone,
                    )
                }
                Text(
                    text = stringResource(R.string.stats_title),
                    style = ResonateTheme.type.displaySm,
                    color = colors.bone,
                )
            }
        }
        item(key = "numbers", contentType = "numbers") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plays.toString(),
                        style = ResonateTheme.type.displayLg,
                        color = colors.accent,
                    )
                    Text(
                        text = stringResource(R.string.stats_plays_this_month),
                        style = ResonateTheme.type.caption,
                        color = colors.muted,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = minutes.toString(),
                        style = ResonateTheme.type.displayLg,
                        color = colors.mint,
                    )
                    Text(
                        text = stringResource(R.string.stats_minutes_this_month),
                        style = ResonateTheme.type.caption,
                        color = colors.muted,
                    )
                }
            }
        }
        if (plays == 0) {
            item(key = "empty", contentType = "empty") {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = ResonateTheme.type.body,
                    color = colors.muted,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (topArtists.isNotEmpty()) {
            item(key = "artists-header", contentType = "eyebrow") {
                Text(
                    text = stringResource(R.string.stats_top_artists).uppercase(),
                    style = ResonateTheme.type.caption,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(
                count = topArtists.size,
                key = { "artist-${topArtists[it].artistId}" },
                contentType = { "stat-artist" },
            ) { index ->
                val artist = topArtists[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = ResonateTheme.type.mono,
                        color = colors.accent,
                    )
                    Text(
                        text = artist.artistName,
                        style = ResonateTheme.type.title,
                        color = colors.bone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(R.string.stats_plays, artist.plays),
                        style = ResonateTheme.type.body,
                        color = colors.muted,
                    )
                }
            }
        }
        if (topSongs.isNotEmpty()) {
            item(key = "songs-header", contentType = "eyebrow") {
                Text(
                    text = stringResource(R.string.stats_top_songs).uppercase(),
                    style = ResonateTheme.type.caption,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(
                count = topSongs.size,
                key = { "song-${topSongs[it].songId}" },
                contentType = { "stat-song" },
            ) { index ->
                val song = topSongs[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(
                        uri = artworkUriFor(song.albumId),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = song.title,
                            style = ResonateTheme.type.title,
                            color = colors.bone,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artistName,
                            style = ResonateTheme.type.body,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(R.string.stats_plays, song.plays),
                        style = ResonateTheme.type.body,
                        color = colors.muted,
                    )
                }
            }
        }
        item(key = "footnote", contentType = "caption") {
            Text(
                text = stringResource(R.string.stats_footnote),
                style = ResonateTheme.type.caption,
                color = colors.muted,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
