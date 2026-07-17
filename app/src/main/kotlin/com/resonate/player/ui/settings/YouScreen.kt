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
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
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

private val appearanceGroup = SettingsGroup(R.string.settings_appearance, Icons.Outlined.Palette)
private val libraryGroup = SettingsGroup(R.string.settings_library, Icons.Outlined.LibraryMusic)
private val equalizerGroup = SettingsGroup(R.string.settings_equalizer, Icons.Outlined.Equalizer)
private val storageGroup = SettingsGroup(R.string.settings_storage, Icons.Outlined.Storage)
private val aboutGroup = SettingsGroup(R.string.settings_about, Icons.Outlined.Info)

@Composable
fun YouScreen(
    onAppearance: () -> Unit,
    onLibrary: () -> Unit,
    onEqualizer: () -> Unit,
    onAbout: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val context = LocalContext.current
    var cacheCleared by remember { mutableStateOf(false) }

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
                SettingsRow(appearanceGroup, onClick = onAppearance)
                SettingsRow(libraryGroup, onClick = onLibrary)
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
