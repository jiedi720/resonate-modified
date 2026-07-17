package com.resonate.player.ui.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onSongClick: (Song, Int) -> Unit,
) {
    val viewModel: PlaylistDetailViewModel = hiltViewModel()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        uri?.let { viewModel.export(it) { } }
    }

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
                        tint = ResonateTheme.colors.bone,
                    )
                }
                Text(
                    text = playlist?.name.orEmpty(),
                    style = ResonateTheme.type.displaySm,
                    color = ResonateTheme.colors.bone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                        tint = ResonateTheme.colors.muted,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_rename)) },
                        onClick = {
                            menuOpen = false
                            renameOpen = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_export)) },
                        onClick = {
                            menuOpen = false
                            exportLauncher.launch("${playlist?.name ?: "playlist"}.m3u8")
                        },
                    )
                }
            }
        }
        items(
            count = songs.size,
            key = { "${songs[it].id}-$it" },
            contentType = { "track" },
        ) { index ->
            val song = songs[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (draggingIndex == index) dragOffsetPx else 0f
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackRow(
                    title = song.title,
                    subtitle = song.artist,
                    duration = formatDuration(song.durationMs),
                    artworkUri = song.artworkUri,
                    supported = song.isSupported,
                    onClick = { onSongClick(song, index) },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.cd_drag_to_reorder),
                    tint = ResonateTheme.colors.muted,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .pointerInput(index, songs.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetPx = 0f
                                },
                                onDrag = { _, amount -> dragOffsetPx += amount.y },
                                onDragEnd = {
                                    val shift = (dragOffsetPx / rowHeightPx).toInt()
                                    val target = (index + shift).coerceIn(0, songs.size - 1)
                                    if (target != index) viewModel.move(index, target)
                                    draggingIndex = -1
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dragOffsetPx = 0f
                                },
                            )
                        },
                )
            }
        }
    }

    if (renameOpen) {
        NameDialog(
            title = stringResource(R.string.playlist_rename),
            initial = playlist?.name.orEmpty(),
            onConfirm = { name ->
                viewModel.rename(name)
                renameOpen = false
            },
            onDismiss = { renameOpen = false },
        )
    }
}
