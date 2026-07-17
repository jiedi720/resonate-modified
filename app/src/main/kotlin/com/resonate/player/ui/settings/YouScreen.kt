package com.resonate.player.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Storage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonate.player.data.repo.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonate.player.R
import com.resonate.player.ui.theme.ResonateTheme

private data class SettingsGroup(val labelRes: Int, val icon: ImageVector)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {
    fun export(target: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repository.export(target)) }
    }

    fun import(source: android.net.Uri, onDone: (Int?) -> Unit) {
        viewModelScope.launch { onDone(repository.import(source)) }
    }
}

private val statsGroup = SettingsGroup(R.string.stats_title, Icons.Outlined.Insights)
private val exportGroup = SettingsGroup(R.string.backup_export, Icons.Outlined.FileUpload)
private val importGroup = SettingsGroup(R.string.backup_import, Icons.Outlined.FileDownload)
private val appearanceGroup = SettingsGroup(R.string.settings_appearance, Icons.Outlined.Palette)
private val libraryGroup = SettingsGroup(R.string.settings_library, Icons.Outlined.LibraryMusic)
private val playbackGroup = SettingsGroup(R.string.settings_playback, Icons.Outlined.PlayCircle)
private val equalizerGroup = SettingsGroup(R.string.settings_equalizer, Icons.Outlined.Equalizer)
private val storageGroup = SettingsGroup(R.string.settings_storage, Icons.Outlined.Storage)
private val aboutGroup = SettingsGroup(R.string.settings_about, Icons.Outlined.Info)

@Composable
fun YouScreen(
    onAppearance: () -> Unit,
    onLibrary: () -> Unit,
    onPlayback: () -> Unit,
    onEqualizer: () -> Unit,
    onAbout: () -> Unit,
    onStats: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val context = LocalContext.current
    var cacheCleared by remember { mutableStateOf(false) }

    val backupViewModel: BackupViewModel = hiltViewModel()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportDone = stringResource(R.string.backup_export_done)
    val importFailed = stringResource(R.string.backup_import_failed)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { backupViewModel.export(it) { ok -> backupMessage = if (ok) exportDone else importFailed } }
    }
    val importResultText = stringResource(R.string.backup_import_done)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            backupViewModel.import(it) { count ->
                backupMessage = if (count != null) importResultText.format(count) else importFailed
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            Text(
                text = stringResource(R.string.tab_you),
                style = ResonateTheme.type.displaySm,
                color = colors.bone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        item(key = "rows", contentType = "settings-rows") {
            Column {
                SettingsRow(statsGroup, onClick = onStats)
                SettingsRow(appearanceGroup, onClick = onAppearance)
                SettingsRow(libraryGroup, onClick = onLibrary)
                SettingsRow(playbackGroup, onClick = onPlayback)
                SettingsRow(equalizerGroup, onClick = onEqualizer)
                SettingsRow(
                    group = storageGroup,
                    subtitle = stringResource(
                        if (cacheCleared) R.string.storage_cache_cleared
                        else R.string.storage_clear_cache
                    ),
                    chevron = false,
                    onClick = {
                        coil3.SingletonImageLoader.get(context).apply {
                            memoryCache?.clear()
                            diskCache?.clear()
                        }
                        cacheCleared = true
                    },
                )
                SettingsRow(
                    group = exportGroup,
                    subtitle = backupMessage,
                    chevron = false,
                    onClick = { exportLauncher.launch("resonate-backup.json") },
                )
                SettingsRow(
                    group = importGroup,
                    chevron = false,
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
                SettingsRow(aboutGroup, onClick = onAbout)
            }
        }
        item(key = "about-line", contentType = "caption") {
            Text(
                text = stringResource(R.string.about_no_internet),
                style = ResonateTheme.type.caption,
                color = colors.muted,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(
    group: SettingsGroup,
    onClick: () -> Unit,
    subtitle: String? = null,
    chevron: Boolean = true,
) {
    val colors = ResonateTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = group.icon,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(group.labelRes),
                style = ResonateTheme.type.title,
                color = colors.bone,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ResonateTheme.type.body,
                    color = colors.muted,
                )
            }
        }
        if (chevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
            )
        }
    }
}
