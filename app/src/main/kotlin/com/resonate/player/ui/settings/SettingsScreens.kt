package com.resonate.player.ui.settings

import android.media.audiofx.Equalizer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.data.prefs.ThemeMode
import com.resonate.player.data.prefs.UserPrefs
import com.resonate.player.ui.library.LibraryScanViewModel
import com.resonate.player.ui.theme.ResonateTheme
import com.resonate.player.ui.theme.ThemeViewModel

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
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
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = ResonateTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = ResonateTheme.type.title, color = colors.bone)
            if (subtitle != null) {
                Text(text = subtitle, style = ResonateTheme.type.body, color = colors.muted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accent,
                checkedThumbColor = colors.onAccent,
            ),
        )
    }
}

// ---------- Appearance (§2.7) ----------

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val viewModel: ThemeViewModel = hiltViewModel()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val colors = ResonateTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(stringResource(R.string.settings_appearance), onBack)

        Text(
            text = stringResource(R.string.appearance_theme).uppercase(),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
        )
        listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        ).forEach { (mode, labelRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { viewModel.update { it.copy(themeMode = mode) } }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = prefs.themeMode == mode,
                    onClick = { viewModel.update { it.copy(themeMode = mode) } },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                )
                Text(
                    text = stringResource(labelRes),
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        SwitchRow(
            title = stringResource(R.string.appearance_amoled),
            subtitle = stringResource(R.string.appearance_amoled_hint),
            checked = prefs.amoledBlack,
            onCheckedChange = { value -> viewModel.update { it.copy(amoledBlack = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.appearance_chroma),
            subtitle = stringResource(R.string.appearance_chroma_hint),
            checked = prefs.chromaEnabled,
            onCheckedChange = { value -> viewModel.update { it.copy(chromaEnabled = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.appearance_material_you),
            subtitle = stringResource(R.string.appearance_material_you_hint),
            checked = prefs.materialYou,
            onCheckedChange = { value -> viewModel.update { it.copy(materialYou = value) } },
        )
    }
}

// ---------- Library (§2.7) ----------

@Composable
fun LibrarySettingsScreen(onBack: () -> Unit) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val scanViewModel: LibraryScanViewModel = hiltViewModel()
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val songCount by scanViewModel.songCount.collectAsStateWithLifecycle()
    val colors = ResonateTheme.colors
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(stringResource(R.string.settings_library), onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { scanViewModel.rescan() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_rescan),
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                )
                Text(
                    text = stringResource(R.string.songs_count, songCount),
                    style = ResonateTheme.type.body,
                    color = colors.muted,
                )
            }
        }

        Text(
            text = stringResource(R.string.library_min_duration).uppercase(),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val options = listOf(0, 1, 15, 30, 60, 120)
            items(count = options.size, key = { options[it] }) { i ->
                val seconds = options[i]
                FilterChip(
                    selected = (if (prefs.durationFilterExplicitlySet) prefs.minDurationSec else 0) == seconds,
                    onClick = {
                        themeViewModel.update {
                            it.copy(minDurationSec = seconds, durationFilterExplicitlySet = true)
                        }
                    },
                    shape = CircleShape,
                    label = {
                        Text(
                            text = if (seconds == 0) stringResource(R.string.duration_all)
                            else stringResource(R.string.duration_seconds, seconds),
                            style = ResonateTheme.type.label,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceRaised,
                        selectedLabelColor = colors.bone,
                        labelColor = colors.muted,
                    ),
                )
            }
        }
        Text(
            text = stringResource(R.string.library_min_duration_hint),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(16.dp),
        )

        // Learning library folder: the selected SAF permission is persisted across restarts.
        val learningFolderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let { treeUri ->
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                    val path = treePathOf(treeUri)
                    if (documentId.substringBefore(':') == "primary" && path != null) {
                        // Persist the selected directory before starting the scan; otherwise
                        // the scanner can read the previous preference and scan the wrong scope.
                        themeViewModel.update {
                            it.copy(
                                learningFolderTreeUris = (it.learningFolderTreeUris + listOfNotNull(it.learningFolderTreeUri) + treeUri.toString()).distinct(),
                                learningFolderTreeUri = null,
                                initialFolderSetupCompleted = true,
                            )
                        }.invokeOnCompletion { cause ->
                            if (cause == null) scanViewModel.rescan()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please choose a folder in internal shared storage.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } catch (_: SecurityException) {
                    Toast.makeText(
                        context,
                        "Folder access could not be saved. Please choose the folder again.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

        val learningFolderUris = (prefs.learningFolderTreeUris + listOfNotNull(prefs.learningFolderTreeUri)).distinct()

        var statusFolderBeingConfigured by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
        val statusFolderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { treeUri ->
            val settingKey = statusFolderBeingConfigured
            statusFolderBeingConfigured = null
            if (treeUri != null && settingKey != null) {
                try {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                    val path = treePathOf(treeUri)
                    if (path == null) {
                        Toast.makeText(context, "Please choose a folder in internal shared storage.", Toast.LENGTH_LONG).show()
                    } else if (learningFolderUris.none { selected ->
                            val rootPath = treePathOf(android.net.Uri.parse(selected))
                            rootPath != null && (path == rootPath || path.startsWith("$rootPath/"))
                        }) {
                        Toast.makeText(
                            context,
                            "Please choose a destination inside one of your selected learning library folders so the audio stays in the library.",
                            Toast.LENGTH_LONG,
                        ).show()
                    } else {
                        themeViewModel.update {
                            when (settingKey) {
                                "mastered" -> it.copy(masteredFolderTreeUri = treeUri.toString())
                                "learning" -> it.copy(learningInProgressFolderTreeUri = treeUri.toString())
                                else -> it.copy(reviewFolderTreeUri = treeUri.toString())
                            }
                        }
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Folder access could not be saved. Please choose the folder again.", Toast.LENGTH_LONG).show()
                }
            }
        }
        Text(
            text = "LEARNING LIBRARY FOLDERS",
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        if (learningFolderUris.isEmpty()) {
            Text(
                text = "Not set — choose one or more folders to scan",
                style = ResonateTheme.type.body,
                color = colors.bone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Column {
                learningFolderUris.forEach { selectedUri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = treePathOf(android.net.Uri.parse(selectedUri)) ?: "Selected folder (path unavailable)",
                            style = ResonateTheme.type.body,
                            color = colors.bone,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        )
                        IconButton(onClick = {
                            themeViewModel.update {
                                val remaining = (it.learningFolderTreeUris + listOfNotNull(it.learningFolderTreeUri))
                                    .distinct().filterNot { uri -> uri == selectedUri }
                                it.copy(learningFolderTreeUris = remaining, learningFolderTreeUri = null)
                            }.invokeOnCompletion { cause ->
                                if (cause == null) scanViewModel.rescan()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove selected folder",
                                tint = colors.muted,
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { learningFolderLauncher.launch(null) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.accent,
            )
            Text(
                text = "Add learning folder",
                style = ResonateTheme.type.title,
                color = colors.bone,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        if (learningFolderUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        themeViewModel.update {
                            it.copy(learningFolderTreeUris = emptyList(), learningFolderTreeUri = null)
                        }.invokeOnCompletion { cause ->
                            if (cause == null) scanViewModel.rescan()
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.muted,
                )
                Text(
                    text = "Clear all learning folders (stop scanning)",
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        Text(
            text = "LEARNING STATUS DESTINATION FOLDERS",
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        )
        Text(
            text = "Choose each destination manually. Keep the folders inside one of your selected learning library folders so moved audio remains visible in the library.",
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        listOf(
            Triple("mastered", "已掌握", prefs.masteredFolderTreeUri),
            Triple("learning", "未掌握 / 学习中", prefs.learningInProgressFolderTreeUri),
            Triple("review", "再复习", prefs.reviewFolderTreeUri),
        ).forEach { (key, label, selectedUri) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        statusFolderBeingConfigured = key
                        statusFolderLauncher.launch(selectedUri?.let(android.net.Uri::parse))
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = ResonateTheme.type.title, color = colors.bone)
                    Text(
                        text = selectedUri?.let { treePathOf(android.net.Uri.parse(it)) } ?: "未设置，点击选择文件夹",
                        style = ResonateTheme.type.caption,
                        color = colors.muted,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Filled.Add, contentDescription = "选择$label文件夹", tint = colors.accent)
            }
        }

        Text(
            text = "Audio in every selected folder and its subfolders will be indexed. Select folders in internal shared storage.",
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(16.dp),
        )

        // Existing excluded-folder controls remain available.
        val folderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let { treeUri ->
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                    treePathOf(treeUri)?.let { path ->
                        themeViewModel.update {
                            if (path in it.excludedFolders) it
                            else it.copy(excludedFolders = it.excludedFolders + path)
                        }
                        scanViewModel.rescan()
                    }
                } catch (_: SecurityException) {
                    Toast.makeText(context, "Folder access could not be saved.", Toast.LENGTH_LONG).show()
                }
            }
        }

        Text(
            text = stringResource(R.string.library_excluded_folders).uppercase(),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        prefs.excludedFolders.forEach { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path,
                    style = ResonateTheme.type.body,
                    color = colors.bone,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        themeViewModel.update {
                            it.copy(excludedFolders = it.excludedFolders - path)
                        }
                        scanViewModel.rescan()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.library_exclude_remove),
                        tint = colors.muted,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { folderLauncher.launch(null) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.accent,
            )
            Text(
                text = stringResource(R.string.library_exclude_add),
                style = ResonateTheme.type.title,
                color = colors.bone,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** SAF tree uri → filesystem path ("primary:Music/X" → /storage/emulated/0/Music/X). */
private fun treePathOf(treeUri: android.net.Uri): String? = try {
    val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
    val (volume, path) = docId.split(':', limit = 2).let {
        it[0] to (it.getOrNull(1) ?: "")
    }
    when (volume) {
        "primary" -> "/storage/emulated/0/$path".trimEnd('/')
        else -> "/storage/$volume/$path".trimEnd('/')
    }
} catch (_: Exception) {
    null
}

// ---------- Playback (§2.7, wave 2) ----------

@Composable
fun PlaybackSettingsScreen(onBack: () -> Unit) {
    val viewModel: ThemeViewModel = hiltViewModel()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val colors = ResonateTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(stringResource(R.string.settings_playback), onBack)

        // Crossfade 0–12s
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.playback_crossfade),
                style = ResonateTheme.type.title,
                color = colors.bone,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (prefs.crossfadeSec == 0) stringResource(R.string.playback_crossfade_off)
                else stringResource(R.string.duration_seconds, prefs.crossfadeSec),
                style = ResonateTheme.type.mono,
                color = colors.accent,
            )
        }
        Slider(
            value = prefs.crossfadeSec.toFloat(),
            onValueChange = { value ->
                viewModel.update { it.copy(crossfadeSec = value.toInt()) }
            },
            valueRange = 0f..12f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceRaised,
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SwitchRow(
            title = stringResource(R.string.playback_skip_silence),
            subtitle = stringResource(R.string.playback_skip_silence_hint),
            checked = prefs.skipSilence,
            onCheckedChange = { value -> viewModel.update { it.copy(skipSilence = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.playback_resume_on_connect),
            subtitle = stringResource(R.string.playback_resume_on_connect_hint),
            checked = prefs.resumeOnConnect,
            onCheckedChange = { value -> viewModel.update { it.copy(resumeOnConnect = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.playback_long_audio_memory),
            subtitle = stringResource(R.string.playback_long_audio_memory_hint),
            checked = prefs.longAudioMemory,
            onCheckedChange = { value -> viewModel.update { it.copy(longAudioMemory = value) } },
        )

        // Replay gain (§2.7): off / track / album
        Text(
            text = stringResource(R.string.playback_replay_gain).uppercase(),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
        )
        listOf(
            com.resonate.player.data.prefs.ReplayGainMode.OFF to R.string.replay_gain_off,
            com.resonate.player.data.prefs.ReplayGainMode.TRACK to R.string.replay_gain_track,
            com.resonate.player.data.prefs.ReplayGainMode.ALBUM to R.string.replay_gain_album,
        ).forEach { (mode, labelRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { viewModel.update { it.copy(replayGainMode = mode) } }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = prefs.replayGainMode == mode,
                    onClick = { viewModel.update { it.copy(replayGainMode = mode) } },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                )
                Text(
                    text = stringResource(labelRes),
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.playback_replay_gain_hint),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ---------- Equalizer (§2.7) ----------

private class EqCapabilities(
    val bandCount: Int,
    val levelRange: IntRange,
    val bandFreqLabels: List<String>,
    val presetNames: List<String>,
)

private fun probeEqualizer(): EqCapabilities? = try {
    val probe = Equalizer(0, 0)
    try {
        val bands = probe.numberOfBands.toInt()
        val range = probe.bandLevelRange
        EqCapabilities(
            bandCount = bands,
            levelRange = range[0].toInt()..range[1].toInt(),
            bandFreqLabels = (0 until bands).map { band ->
                val hz = probe.getCenterFreq(band.toShort()) / 1000
                if (hz >= 1000) "${hz / 1000}k" else "$hz"
            },
            presetNames = (0 until probe.numberOfPresets).map {
                probe.getPresetName(it.toShort())
            },
        )
    } finally {
        probe.release()
    }
} catch (_: Exception) {
    null
}

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val viewModel: ThemeViewModel = hiltViewModel()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val colors = ResonateTheme.colors
    val caps = remember { probeEqualizer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(stringResource(R.string.settings_equalizer), onBack)

        SwitchRow(
            title = stringResource(R.string.eq_enabled),
            subtitle = null,
            checked = prefs.eqEnabled,
            onCheckedChange = { value -> viewModel.update { it.copy(eqEnabled = value) } },
        )

        if (caps == null) {
            Text(
                text = stringResource(R.string.eq_unavailable),
                style = ResonateTheme.type.body,
                color = colors.muted,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        if (caps.presetNames.isNotEmpty()) {
            Text(
                text = stringResource(R.string.eq_presets).uppercase(),
                style = ResonateTheme.type.caption,
                color = colors.muted,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(count = caps.presetNames.size + 1, key = { it }) { i ->
                    val preset = i - 1 // -1 = custom
                    FilterChip(
                        selected = prefs.eqPreset == preset,
                        onClick = { viewModel.update { it.copy(eqPreset = preset) } },
                        shape = CircleShape,
                        enabled = prefs.eqEnabled,
                        label = {
                            Text(
                                text = if (preset < 0) stringResource(R.string.eq_custom)
                                else caps.presetNames[preset],
                                style = ResonateTheme.type.label,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.surfaceRaised,
                            selectedLabelColor = colors.bone,
                            labelColor = colors.muted,
                        ),
                    )
                }
            }
        }

        // Band sliders (custom mode)
        repeat(caps.bandCount) { band ->
            val level = prefs.eqBandLevels.getOrNull(band) ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = caps.bandFreqLabels[band],
                    style = ResonateTheme.type.mono,
                    color = colors.muted,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Slider(
                    value = level.toFloat(),
                    onValueChange = { value ->
                        viewModel.update {
                            val levels = MutableList(caps.bandCount) { i ->
                                it.eqBandLevels.getOrNull(i) ?: 0
                            }
                            levels[band] = value.toInt()
                            it.copy(eqBandLevels = levels, eqPreset = -1)
                        }
                    },
                    valueRange = caps.levelRange.first.toFloat()..caps.levelRange.last.toFloat(),
                    enabled = prefs.eqEnabled && prefs.eqPreset < 0,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.surfaceRaised,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Bass boost & virtualizer (§2.7)
        listOf(
            Triple(R.string.eq_bass_boost, prefs.bassBoost) { v: Int ->
                viewModel.update { it.copy(bassBoost = v) }
            },
            Triple(R.string.eq_virtualizer, prefs.virtualizer) { v: Int ->
                viewModel.update { it.copy(virtualizer = v) }
            },
        ).forEach { (labelRes, value, setter) ->
            Text(
                text = stringResource(labelRes),
                style = ResonateTheme.type.title,
                color = colors.bone,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { setter(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = prefs.eqEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceRaised,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// ---------- About (§2.7) ----------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = ResonateTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(stringResource(R.string.settings_about), onBack)
        Text(
            text = stringResource(R.string.app_name),
            style = ResonateTheme.type.displayLg,
            color = colors.bone,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.about_version,
                com.resonate.player.BuildConfig.VERSION_NAME,
            ),
            style = ResonateTheme.type.mono,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.about_no_internet),
            style = ResonateTheme.type.body,
            color = colors.bone,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            text = stringResource(R.string.about_license),
            style = ResonateTheme.type.title,
            color = colors.bone,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.about_license_body),
            style = ResonateTheme.type.body,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.about_whats_new).uppercase(),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Text(
            text = stringResource(R.string.about_changelog),
            style = ResonateTheme.type.body,
            color = colors.bone,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            text = stringResource(R.string.about_licenses_body),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
