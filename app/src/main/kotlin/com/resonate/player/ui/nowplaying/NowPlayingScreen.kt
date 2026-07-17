package com.resonate.player.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.domain.model.formatDuration
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.components.TransportButton
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.queue.QueueSheet
import com.resonate.player.ui.theme.ResonateTheme

/**
 * §2.4. Expands from the mini-player; transport lives in the bottom third
 * (one-handed by design). Chroma background arrives in step 6.
 */
@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    onCollapse: () -> Unit,
    onAlbumClick: (Long) -> Unit,
) {
    val colors = ResonateTheme.colors
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle(0L)
    val haptics = LocalHapticFeedback.current

    // Close when the queue ends.
    LaunchedEffect(nowPlaying) {
        if (nowPlaying == null) onCollapse()
    }
    val track = nowPlaying ?: return

    var scrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var lastDetent by remember { mutableStateOf(-1) }

    // §1.1: chroma bleeds into the Now Playing background as a top gradient.
    val backgroundBrush = Brush.verticalGradient(
        0f to colors.accent.copy(alpha = 0.28f),
        0.55f to colors.ink,
        1f to colors.ink,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onVerticalDrag = { _, dragAmount -> accumulated += dragAmount },
                    onDragEnd = {
                        if (accumulated > 160.dp.toPx()) onCollapse()
                    },
                )
            },
    ) {
        // Collapse handle
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_collapse),
                    tint = colors.muted,
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Artwork — swipe horizontally to change track (§2.4)
        ArtworkImage(
            uri = track.artworkUri,
            contentDescription = stringResource(R.string.cd_artwork),
            cornerRadius = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onHorizontalDrag = { _, dragAmount -> accumulated += dragAmount },
                        onDragEnd = {
                            val threshold = 120.dp.toPx()
                            when {
                                accumulated < -threshold -> viewModel.next()
                                accumulated > threshold -> viewModel.previous()
                            }
                        },
                    )
                },
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // §1.3: wrap to two lines, marquee only if it still overflows.
        var titleOverflows by remember(track.title) { mutableStateOf(false) }
        if (titleOverflows) {
            Text(
                text = track.title,
                style = ResonateTheme.type.displayLg,
                color = colors.bone,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
        } else {
            Text(
                text = track.title,
                style = ResonateTheme.type.displayLg,
                color = colors.bone,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (it.hasVisualOverflow) titleOverflows = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        Text(
            text = listOf(track.artist, track.album)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = ResonateTheme.type.body,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(enabled = track.albumId != null) {
                    track.albumId?.let(onAlbumClick)
                },
        )

        // Seek bar: 4dp track, mono timecodes at both ends, detents at 25/50/75 (§2.4)
        val displayPosition = if (scrubbing) scrubPosition.toLong() else positionMs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(displayPosition),
                style = ResonateTheme.type.mono,
                color = colors.muted,
            )
            Slider(
                value = displayPosition.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { value ->
                    scrubbing = true
                    scrubPosition = value
                    if (durationMs > 0) {
                        val detent = (value / durationMs * 4).toInt()
                        if (detent in 1..3 && detent != lastDetent) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        lastDetent = detent
                    }
                },
                onValueChangeFinished = {
                    viewModel.seekTo(scrubPosition.toLong())
                    scrubbing = false
                    lastDetent = -1
                },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceRaised,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = formatDuration(durationMs),
                style = ResonateTheme.type.mono,
                color = colors.muted,
            )
        }

        // Transport (§2.4): shuffle · prev · play 72dp chroma-filled · next · repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.cd_shuffle),
                tint = if (shuffleEnabled) colors.accent else colors.muted,
                onClick = viewModel::toggleShuffle,
            )
            TransportButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.cd_previous),
                size = 56.dp,
                iconSize = 32.dp,
                tint = colors.bone,
                onClick = viewModel::previous,
            )
            TransportButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.cd_pause else R.string.cd_play
                ),
                size = 72.dp,
                iconSize = 36.dp,
                tint = colors.onAccent,
                container = colors.accent,
                onClick = viewModel::playPause,
            )
            TransportButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.cd_next),
                size = 56.dp,
                iconSize = 32.dp,
                tint = colors.bone,
                onClick = viewModel::next,
            )
            TransportButton(
                icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) {
                    Icons.Filled.RepeatOne
                } else {
                    Icons.Filled.Repeat
                },
                contentDescription = stringResource(R.string.cd_repeat),
                tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
                    colors.muted
                } else {
                    colors.accent
                },
                onClick = viewModel::cycleRepeatMode,
            )
        }

        // Secondary row (§2.4): favorite · lyrics (if embedded) · sleep · speed · queue
        var queueOpen by remember { mutableStateOf(false) }
        var sleepOpen by remember { mutableStateOf(false) }
        var speedOpen by remember { mutableStateOf(false) }
        var lyricsOpen by remember { mutableStateOf(false) }
        val sleepEndsAt by viewModel.sleepEndsAt.collectAsStateWithLifecycle()
        val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = viewModel::toggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (isFavorite) colors.accent else colors.muted,
                )
            }
            if (lyrics != null) {
                IconButton(onClick = { lyricsOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.Lyrics,
                        contentDescription = stringResource(R.string.cd_lyrics),
                        tint = colors.muted,
                    )
                }
            }
            IconButton(onClick = { sleepOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.NightsStay,
                    contentDescription = stringResource(R.string.settings_sleep_timer),
                    tint = if (sleepEndsAt != null) colors.accent else colors.muted,
                )
            }
            IconButton(onClick = { speedOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = stringResource(R.string.cd_speed),
                    tint = colors.muted,
                )
            }
            IconButton(onClick = { queueOpen = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = stringResource(R.string.cd_queue),
                    tint = colors.muted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (queueOpen) {
            QueueSheet(viewModel = viewModel, onDismiss = { queueOpen = false })
        }
        if (sleepOpen) {
            SleepTimerSheet(viewModel = viewModel, onDismiss = { sleepOpen = false })
        }
        if (speedOpen) {
            SpeedSheet(viewModel = viewModel, onDismiss = { speedOpen = false })
        }
        lyrics?.let { current ->
            if (lyricsOpen) {
                LyricsSheet(
                    viewModel = viewModel,
                    lyrics = current,
                    onDismiss = { lyricsOpen = false },
                )
            }
        }
    }
}
