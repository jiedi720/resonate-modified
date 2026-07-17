package com.resonate.player.ui.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.data.db.PlaylistRow
import com.resonate.player.ui.components.ArtworkImage
import com.resonate.player.ui.theme.ResonateTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 2×2 mosaic of the first four tracks' artwork (§2.6). */
@Composable
fun PlaylistMosaic(
    artUris: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        repeat(2) { row ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(2) { col ->
                    val index = row * 2 + col
                    ArtworkImage(
                        uri = artUris.getOrNull(index),
                        contentDescription = null,
                        cornerRadius = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(0.5.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTabContent(
    onPlaylistClick: (Long) -> Unit,
) {
    val viewModel: PlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val mosaics by viewModel.mosaics.collectAsStateWithLifecycle()

    var createOpen by rememberSaveable { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<PlaylistRow?>(null) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> importUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { createOpen = true },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResonateTheme.colors.surfaceRaised,
                    contentColor = ResonateTheme.colors.bone,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.playlist_new),
                    style = ResonateTheme.type.label,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Button(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResonateTheme.colors.surfaceRaised,
                    contentColor = ResonateTheme.colors.bone,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.playlist_import),
                    style = ResonateTheme.type.label,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_playlists),
                    style = ResonateTheme.type.body,
                    color = ResonateTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    count = playlists.size,
                    key = { playlists[it].id },
                    contentType = { "playlist" },
                ) { index ->
                    val playlist = playlists[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .combinedClickable(
                                onClick = { onPlaylistClick(playlist.id) },
                                onLongClick = { actionsFor = playlist },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistMosaic(
                            artUris = mosaics[playlist.id] ?: persistentListOf(),
                            modifier = Modifier.size(48.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text(
                                text = playlist.name,
                                style = ResonateTheme.type.title,
                                color = ResonateTheme.colors.bone,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.songs_count, playlist.songCount),
                                style = ResonateTheme.type.body,
                                color = ResonateTheme.colors.muted,
                            )
                        }
                    }
                }
            }
        }
    }

    if (createOpen) {
        NameDialog(
            title = stringResource(R.string.playlist_new),
            initial = "",
            onConfirm = { name ->
                viewModel.create(name)
                createOpen = false
            },
            onDismiss = { createOpen = false },
        )
    }

    importUri?.let { uri ->
        NameDialog(
            title = stringResource(R.string.playlist_import),
            initial = stringResource(R.string.playlist_imported_default_name),
            onConfirm = { name ->
                viewModel.import(uri, name) { }
                importUri = null
            },
            onDismiss = { importUri = null },
        )
    }

    actionsFor?.let { playlist ->
        PlaylistActionsSheet(
            playlist = playlist,
            onRename = { name -> viewModel.rename(playlist.id, name) },
            onDelete = { viewModel.delete(playlist.id) },
            onDismiss = { actionsFor = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistActionsSheet(
    playlist: PlaylistRow,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var renameOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ResonateTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = playlist.name,
                style = ResonateTheme.type.displaySm,
                color = ResonateTheme.colors.bone,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            TextButton(
                onClick = { renameOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.playlist_rename),
                    style = ResonateTheme.type.title,
                    color = ResonateTheme.colors.bone,
                )
            }
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.playlist_delete),
                    style = ResonateTheme.type.title,
                    color = ResonateTheme.colors.pulse,
                )
            }
        }
    }

    if (renameOpen) {
        NameDialog(
            title = stringResource(R.string.playlist_rename),
            initial = playlist.name,
            onConfirm = { name ->
                onRename(name)
                renameOpen = false
                onDismiss()
            },
            onDismiss = { renameOpen = false },
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ResonateTheme.colors.surfaceRaised,
        title = {
            Text(
                text = title,
                style = ResonateTheme.type.displaySm,
                color = ResonateTheme.colors.bone,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = ResonateTheme.type.title.copy(color = ResonateTheme.colors.bone),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
            ) {
                Text(
                    text = stringResource(R.string.action_save),
                    color = ResonateTheme.colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = ResonateTheme.colors.muted,
                )
            }
        },
    )
}
