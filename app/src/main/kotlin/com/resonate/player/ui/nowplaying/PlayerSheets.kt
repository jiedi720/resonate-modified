package com.resonate.player.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.R
import com.resonate.player.playback.PlaybackService
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.theme.ResonateTheme
import kotlin.math.roundToInt

/** §2.7 sleep timer: 5/15/30/60, end of track, cancel; fades the last 20s. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val sleepEndsAt by viewModel.sleepEndsAt.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.settings_sleep_timer),
                style = ResonateTheme.type.displaySm,
                color = colors.bone,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            listOf(5, 15, 30, 60).forEach { minutes ->
                SheetOption(
                    label = stringResource(R.string.sleep_minutes, minutes),
                    onClick = {
                        viewModel.setSleepTimer(minutes)
                        onDismiss()
                    },
                )
            }
            SheetOption(
                label = stringResource(R.string.sleep_end_of_track),
                onClick = {
                    viewModel.setSleepTimer(PlaybackService.SLEEP_END_OF_TRACK)
                    onDismiss()
                },
            )
            if (sleepEndsAt != null) {
                SheetOption(
                    label = stringResource(R.string.sleep_cancel),
                    color = colors.pulse,
                    onClick = {
                        viewModel.setSleepTimer(PlaybackService.SLEEP_CANCEL)
                        onDismiss()
                    },
                )
            }
        }
    }
}

/** §2.4: 0.5×–2.0×, 0.05 steps, pitch correction toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val colors = ResonateTheme.colors
    val persistedSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val pitchCorrection by viewModel.pitchCorrection.collectAsStateWithLifecycle()
    var speed by remember(persistedSpeed) { mutableFloatStateOf(persistedSpeed) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.speed_title),
                    style = ResonateTheme.type.displaySm,
                    color = colors.bone,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.speed_value, speed),
                    style = ResonateTheme.type.mono,
                    color = colors.accent,
                )
            }
            Slider(
                value = speed,
                onValueChange = { value ->
                    // 0.05 steps
                    speed = (value * 20).roundToInt() / 20f
                },
                onValueChangeFinished = {
                    viewModel.setSpeed(speed, pitchCorrection)
                },
                valueRange = 0.5f..2f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceRaised,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.speed_pitch_correction),
                    style = ResonateTheme.type.title,
                    color = colors.bone,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = pitchCorrection,
                    onCheckedChange = { viewModel.setSpeed(speed, it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        checkedThumbColor = colors.onAccent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SheetOption(
    label: String,
    color: androidx.compose.ui.graphics.Color = ResonateTheme.colors.bone,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = ResonateTheme.type.title,
            color = color,
        )
    }
}
