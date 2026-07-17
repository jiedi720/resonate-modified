package com.resonate.player.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.data.lyrics.LrcParser
import com.resonate.player.data.lyrics.Lyrics
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.theme.ResonateTheme

/**
 * §2.4 lyrics (embedded only, §0). Synced lines follow playback with the
 * chroma accent on the active line; tapping a synced line seeks to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    viewModel: PlaybackViewModel,
    lyrics: Lyrics,
    onDismiss: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle(0L)
    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics, positionMs) {
        LrcParser.activeIndex(lyrics, positionMs)
    }

    // Keep the active line in the upper third while singing along.
    LaunchedEffect(activeIndex) {
        if (lyrics.synced && activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(bottom = 24.dp),
        ) {
            items(
                count = lyrics.lines.size,
                key = { it },
                contentType = { "lyric-line" },
            ) { index ->
                val line = lyrics.lines[index]
                val isActive = index == activeIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = lyrics.synced && line.timeMs != null) {
                            line.timeMs?.let(viewModel::seekTo)
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = line.text.ifEmpty { "♪" },
                        style = if (isActive) ResonateTheme.type.displaySm else ResonateTheme.type.title,
                        color = when {
                            isActive -> colors.accent
                            lyrics.synced && activeIndex in 0..Int.MAX_VALUE && index < activeIndex -> colors.muted
                            else -> colors.bone
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun NoLyricsHint() {
    Text(
        text = stringResource(R.string.lyrics_none),
        style = ResonateTheme.type.body,
        color = ResonateTheme.colors.muted,
        modifier = Modifier.padding(24.dp),
    )
}
