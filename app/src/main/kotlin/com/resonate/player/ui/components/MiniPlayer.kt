package com.resonate.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.theme.ResonateTheme

/** §2: 64dp, docked above the tab bar, persists across tabs. Hidden when idle. */
@Composable
fun MiniPlayer(
    viewModel: PlaybackViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ResonateTheme.colors
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val track = nowPlaying ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        // §1.1: the chroma-tinted edge is the mini-player's share of the bleed.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.accent.copy(alpha = 0.75f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onExpand)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                uri = track.artworkUri,
                contentDescription = stringResource(R.string.cd_expand_player),
                modifier = Modifier.size(48.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = track.title,
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = ResonateTheme.type.body,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = viewModel::playPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.cd_pause else R.string.cd_play
                    ),
                    tint = colors.accent,
                )
            }
            IconButton(onClick = viewModel::next) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = colors.bone,
                )
            }
        }
    }
}
