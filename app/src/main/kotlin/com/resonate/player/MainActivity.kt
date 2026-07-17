package com.resonate.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonate.player.ui.RootScaffold
import com.resonate.player.ui.library.LibraryScanViewModel
import com.resonate.player.ui.permission.PermissionGate
import com.resonate.player.ui.theme.ResonateTheme
import com.resonate.player.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
            ResonateTheme(
                themeMode = prefs.themeMode,
                amoledBlack = prefs.amoledBlack,
                materialYou = prefs.materialYou,
            ) {
                val scanViewModel: LibraryScanViewModel = hiltViewModel()
                PermissionGate(onGranted = scanViewModel::onPermissionGranted) {
                    RootScaffold()
                }
            }
        }
    }
}
