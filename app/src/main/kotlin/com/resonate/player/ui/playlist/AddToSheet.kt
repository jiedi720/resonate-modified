package com.resonate.player.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resonate.player.R
import com.resonate.player.data.repo.PlaylistRepository
import com.resonate.player.domain.model.Song
import com.resonate.player.ui.theme.ResonateTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Long-press action sheet: add to queue, add to a playlist, or a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToSheet(
    song: Song,
    onAddToQueue: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: PlaylistsViewModel = hiltViewModel()
    val addViewModel: AddToPlaylistViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val colors = ResonateTheme.colors
    var createOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = song.title,
                style = ResonateTheme.type.displaySm,
                color = colors.bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SheetRow(
                icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = colors.muted) },
                label = stringResource(R.string.add_to_queue),
                onClick = {
                    onAddToQueue(song)
                    onDismiss()
                },
            )
            SheetRow(
                icon = { Icon(Icons.Filled.Add, null, tint = colors.muted) },
                label = stringResource(R.string.playlist_new),
                onClick = { createOpen = true },
            )
            LazyColumn {
                items(
                    count = playlists.size,
                    key = { playlists[it].id },
                    contentType = { "playlist-pick" },
                ) { index ->
                    val playlist = playlists[index]
                    SheetRow(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                null,
                                tint = colors.muted,
                            )
                        },
                        label = playlist.name,
                        onClick = {
                            addViewModel.addTo(playlist.id, song.id)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }

    if (createOpen) {
        NameDialog(
            title = stringResource(R.string.playlist_new),
            initial = "",
            onConfirm = { name ->
                addViewModel.createWith(name, song.id)
                createOpen = false
                onDismiss()
            },
            onDismiss = { createOpen = false },
        )
    }
}

@Composable
private fun SheetRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = label,
            style = ResonateTheme.type.title,
            color = ResonateTheme.colors.bone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {
    fun addTo(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongs(playlistId, listOf(songId)) }
    }

    fun createWith(name: String, songId: Long) {
        viewModelScope.launch { repository.create(name, listOf(songId)) }
    }
}
