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
import androidx.compose.runtime.getValue
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
            val options = listOf(0, 15, 30, 60, 120)
            items(count = options.size, key = { options[it] }) { i ->
                val seconds = options[i]
                FilterChip(
                    selected = prefs.minDurationSec == seconds,
                    onClick = { themeViewModel.update { it.copy(minDurationSec = seconds) } },
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
            text = stringResource(R.string.about_licenses_body),
            style = ResonateTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
