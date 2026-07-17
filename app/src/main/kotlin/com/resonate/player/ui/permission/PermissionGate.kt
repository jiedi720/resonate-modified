package com.resonate.player.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.resonate.player.R
import com.resonate.player.ui.theme.ResonateTheme

val audioPermission: String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

/**
 * §3.1: real rationale screen gating the whole app until the audio permission
 * is granted. Once granted, [onGranted] fires (starts watcher + first scan).
 */
@Composable
fun PermissionGate(
    onGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    if (granted) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onGranted() }
        content()
    } else {
        RationaleScreen(onPermissionGranted = { granted = true })
    }
}

@Composable
private fun RationaleScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val colors = ResonateTheme.colors
    var deniedOnce by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) onPermissionGranted() else deniedOnce = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryMusic,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = ResonateTheme.type.displaySm,
            color = colors.bone,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.permission_rationale),
            style = ResonateTheme.type.body,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = { launcher.launch(audioPermission) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ),
            modifier = Modifier
                .padding(top = 32.dp)
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_grant),
                style = ResonateTheme.type.label,
            )
        }
        if (deniedOnce) {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.permission_open_settings),
                    style = ResonateTheme.type.label,
                    color = colors.muted,
                )
            }
        }
    }
}
