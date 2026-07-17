package com.resonate.player.ui.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.playlist.NameDialog
import com.resonate.player.ui.theme.ResonateTheme

/** §2.5: queue sheet — drag-reorder, swipe-to-remove, clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentIndex by viewModel.queueIndex.collectAsStateWithLifecycle()

    // Drag state: which row is lifted and how far it has traveled.
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var saveOpen by remember { mutableStateOf(false) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    if (saveOpen) {
        NameDialog(
            title = stringResource(R.string.queue_save_as_playlist),
            initial = "",
            onConfirm = { name ->
                viewModel.saveQueueAsPlaylist(name)
                saveOpen = false
            },
            onDismiss = { saveOpen = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.queue_title),
                style = ResonateTheme.type.displaySm,
                color = colors.bone,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { saveOpen = true }) {
                Text(
                    text = stringResource(R.string.queue_save_as_playlist),
                    style = ResonateTheme.type.label,
                    color = colors.muted,
                )
            }
            TextButton(
                onClick = {
                    viewModel.clearQueue()
                    onDismiss()
                },
            ) {
                Text(
                    text = stringResource(R.string.queue_clear),
                    style = ResonateTheme.type.label,
                    color = colors.muted,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                count = queue.size,
                key = { "${queue[it].songId}-$it" },
                contentType = { "queue-row" },
            ) { index ->
                val entry = queue[index]

                if (index == currentIndex + 1) {
                    Text(
                        text = stringResource(R.string.queue_playing_next).uppercase(),
                        style = ResonateTheme.type.caption,
                        color = colors.muted,
                        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                    )
                }

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            viewModel.removeQueueItem(index)
                        }
                        false
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .graphicsLayer {
                                translationY = if (draggingIndex == index) dragOffsetPx else 0f
                            }
                            .clickable { viewModel.playQueueItem(index) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkImage(
                            uri = entry.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text(
                                text = entry.title,
                                style = ResonateTheme.type.title,
                                color = if (index == currentIndex) colors.accent else colors.bone,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.artist,
                                style = ResonateTheme.type.body,
                                color = colors.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.cd_drag_to_reorder),
                            tint = colors.muted,
                            modifier = Modifier.pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetPx = 0f
                                    },
                                    onDrag = { _, amount -> dragOffsetPx += amount.y },
                                    onDragEnd = {
                                        val shift = (dragOffsetPx / rowHeightPx).toInt()
                                        val target = (index + shift).coerceIn(0, queue.size - 1)
                                        if (target != index) viewModel.moveQueueItem(index, target)
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
        }
    }
}
