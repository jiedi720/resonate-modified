package com.resonate.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.ui.RootScaffold
import com.resonate.player.ui.library.LibraryScanViewModel
import com.resonate.player.ui.permission.PermissionGate
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.theme.ResonateTheme
import com.resonate.player.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

/** Commands arriving from outside: share target (§2.8) and app shortcuts. */
private sealed interface LaunchCommand {
    data class PlayUri(val uri: Uri) : LaunchCommand
    data object ShuffleAll : LaunchCommand
    data object Continue : LaunchCommand
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val launchCommand = MutableStateFlow<LaunchCommand?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
            ResonateTheme(
                themeMode = prefs.themeMode,
                amoledBlack = prefs.amoledBlack,
                materialYou = prefs.materialYou,
            ) {
                val playbackViewModel: PlaybackViewModel = hiltViewModel()
                val command by launchCommand.collectAsStateWithLifecycle()
                LaunchedEffect(command) {
                    when (val cmd = command) {
                        is LaunchCommand.PlayUri -> playbackViewModel.playExternal(cmd.uri)
                        LaunchCommand.ShuffleAll -> playbackViewModel.shuffleAll()
                        LaunchCommand.Continue -> playbackViewModel.play()
                        null -> Unit
                    }
                    launchCommand.value = null
                }

                val scanViewModel: LibraryScanViewModel = hiltViewModel()
                PermissionGate(onGranted = scanViewModel::onPermissionGranted) {
                    RootScaffold()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        launchCommand.value = when {
            intent?.action == Intent.ACTION_VIEW && intent.data != null ->
                LaunchCommand.PlayUri(intent.data ?: return)
            intent?.action == "com.resonate.player.SHUFFLE_ALL" -> LaunchCommand.ShuffleAll
            intent?.action == "com.resonate.player.CONTINUE" -> LaunchCommand.Continue
            else -> return
        }
    }
}
