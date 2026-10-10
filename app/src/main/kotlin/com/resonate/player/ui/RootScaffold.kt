package com.resonate.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.resonate.player.R
import com.resonate.player.data.repo.ScanState
import com.resonate.player.ui.theme.ThemeViewModel
import com.resonate.player.ui.components.MiniPlayer
import com.resonate.player.ui.components.Responsive
import com.resonate.player.ui.home.HomeScreen
import com.resonate.player.ui.library.LibraryScanViewModel
import com.resonate.player.ui.library.LibraryScreen
import com.resonate.player.ui.library.detail.AlbumDetailScreen
import com.resonate.player.ui.library.detail.ArtistDetailScreen
import com.resonate.player.ui.library.detail.FolderDetailScreen
import com.resonate.player.ui.library.detail.GenreDetailScreen
import com.resonate.player.ui.navigation.AboutRoute
import com.resonate.player.ui.navigation.AlbumDetailRoute
import com.resonate.player.ui.navigation.AppearanceSettingsRoute
import com.resonate.player.ui.navigation.ArtistDetailRoute
import com.resonate.player.ui.navigation.EqualizerRoute
import com.resonate.player.ui.navigation.FolderDetailRoute
import com.resonate.player.ui.navigation.GenreDetailRoute
import com.resonate.player.ui.navigation.LibrarySettingsRoute
import com.resonate.player.ui.navigation.PlaybackSettingsRoute
import com.resonate.player.ui.navigation.HomeRoute
import com.resonate.player.ui.navigation.LibraryRoute
import com.resonate.player.ui.navigation.PlaylistDetailRoute
import com.resonate.player.ui.navigation.SearchRoute
import com.resonate.player.ui.navigation.StatsRoute
import com.resonate.player.ui.navigation.YouRoute
import com.resonate.player.ui.nowplaying.NowPlayingScreen
import com.resonate.player.ui.player.PlaybackViewModel
import com.resonate.player.ui.playlist.AddToSheet
import com.resonate.player.ui.playlist.PlaylistDetailScreen
import com.resonate.player.ui.search.SearchScreen
import com.resonate.player.ui.settings.AboutScreen
import com.resonate.player.ui.settings.AppearanceSettingsScreen
import com.resonate.player.ui.settings.EqualizerScreen
import com.resonate.player.ui.settings.LibrarySettingsScreen
import com.resonate.player.ui.settings.PlaybackSettingsScreen
import com.resonate.player.ui.settings.YouScreen
import com.resonate.player.ui.theme.ChromaEngine
import com.resonate.player.ui.theme.LocalResonateColors
import com.resonate.player.ui.theme.ResonateTheme
import kotlin.reflect.KClass

private data class TabDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val labelRes: Int,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

private val tabs = listOf(
    TabDestination(HomeRoute, HomeRoute::class, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    TabDestination(LibraryRoute, LibraryRoute::class, R.string.tab_library, Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    TabDestination(SearchRoute, SearchRoute::class, R.string.tab_search, Icons.Outlined.Search, Icons.Filled.Search),
    TabDestination(YouRoute, YouRoute::class, R.string.tab_you, Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun RootScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val scanViewModel: LibraryScanViewModel = hiltViewModel()
    var showFirstFolderPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.initialFolderSetupCompleted, prefs.learningFolderTreeUri) {
        showFirstFolderPrompt = !prefs.initialFolderSetupCompleted && prefs.learningFolderTreeUri == null
    }
    val firstFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
                val documentId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                if (documentId.substringBefore(':') != "primary") {
                    android.widget.Toast.makeText(
                        this, "Please choose a folder in internal shared storage.", android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    themeViewModel.update {
                        it.copy(learningFolderTreeUri = uri.toString(), initialFolderSetupCompleted = true)
                    }.invokeOnCompletion { cause ->
                        if (cause == null) scanViewModel.rescan()
                    }
                    showFirstFolderPrompt = false
                }
            } catch (_: SecurityException) {
                android.widget.Toast.makeText(
                    this, "Folder access could not be saved. Please choose the folder again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // §9: POST_NOTIFICATIONS is runtime on 33+ — ask once, at first play intent.
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var askedForNotifications by remember { mutableStateOf(false) }
    fun beforePlay() {
        if (Build.VERSION.SDK_INT >= 33 && !askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var nowPlayingOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }

    // §1.1 chroma bleed: artwork colors become the accent, clamped for AA
    // contrast, cross-faded over 600ms (§1.4's third and last animation).
    val chroma by playbackViewModel.chroma.collectAsStateWithLifecycle()
    val baseColors = ResonateTheme.colors
    val (targetAccent, targetOnAccent) = remember(chroma, baseColors) {
        chroma?.let {
            val (accent, onAccent) = ChromaEngine.clampForTheme(
                raw = it.primary,
                isDark = baseColors.isDark,
                background = baseColors.ink.toArgb(),
                onDarkText = baseColors.ink.toArgb(),
                onLightText = baseColors.bone.toArgb(),
            )
            Color(accent) to Color(onAccent)
        } ?: (baseColors.accent to baseColors.onAccent)
    }
    val animatedAccent by animateColorAsState(targetAccent, tween(600), label = "chromaAccent")
    val animatedOnAccent by animateColorAsState(targetOnAccent, tween(600), label = "chromaOnAccent")

    CompositionLocalProvider(
        LocalResonateColors provides baseColors.copy(
            accent = animatedAccent,
            onAccent = animatedOnAccent,
        ),
    ) {
        Box {
        MainScaffold(
            navController = navController,
            currentDestination = currentDestination,
            playbackViewModel = playbackViewModel,
            beforePlay = ::beforePlay,
            onExpandPlayer = { nowPlayingOpen = true },
        )
        // §1.4 motion: mini-player → Now Playing expand, 400ms emphasized easing.
        if (showFirstFolderPrompt) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.first_folder_title)) },
                text = { Text(stringResource(R.string.first_folder_body)) },
                confirmButton = {
                    TextButton(onClick = { firstFolderLauncher.launch(null) }) {
                        Text(stringResource(R.string.first_folder_choose))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            themeViewModel.update { it.copy(initialFolderSetupCompleted = true) }
                            showFirstFolderPrompt = false
                        }
                    ) {
                        Text(stringResource(R.string.first_folder_later))
                    }
                },
            )
        }
        AnimatedVisibility(
            visible = nowPlayingOpen,
            enter = slideInVertically(
                animationSpec = tween(400, easing = EmphasizedEasing),
                initialOffsetY = { it },
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = EmphasizedEasing),
                targetOffsetY = { it },
            ) + fadeOut(tween(200)),
        ) {
                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    onCollapse = { nowPlayingOpen = false },
                    onAlbumClick = { albumId ->
                        nowPlayingOpen = false
                        navController.navigate(AlbumDetailRoute(albumId))
                    },
                )
            }
        }
    }
}

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    playbackViewModel: PlaybackViewModel,
    beforePlay: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    // §4: one-line snackbar when a bad file gets skipped.
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val errorMessage = stringResource(R.string.playback_error)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        playbackViewModel.playbackError.collect {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        containerColor = ResonateTheme.colors.ink,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ResonateTheme.colors.surface)
                    .navigationBarsPadding(),
            ) {
                ScanBanner()
                MiniPlayer(
                    viewModel = playbackViewModel,
                    onExpand = onExpandPlayer,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.routeClass) == true
                        TabItem(
                            tab = tab,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
      // Center all destination content in a readable column on tablets and
      // foldables; the mini-player and tab bar stay full-width for reach.
      Box(
          modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          contentAlignment = Alignment.TopCenter,
      ) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .widthIn(max = Responsive.ListMaxWidth)
                .fillMaxSize(),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onShuffleAll = {
                        beforePlay()
                        playbackViewModel.shuffleAll()
                    },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playFromAllSongs(song.id)
                    },
                )
            }
            composable<LibraryRoute> {
                var addSelection by remember {
                    mutableStateOf<List<com.resonate.player.domain.model.Song>>(emptyList())
                }
                LibraryScreen(
                    onAlbumClick = { navController.navigate(AlbumDetailRoute(it)) },
                    onArtistClick = { navController.navigate(ArtistDetailRoute(it)) },
                    onGenreClick = { navController.navigate(GenreDetailRoute(it)) },
                    onPlaylistClick = { navController.navigate(PlaylistDetailRoute(it)) },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playFromAllSongs(song.id)
                    },
                    onFolderSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playFolder(song.folderId, song.id)
                    },
                    onAddSelection = { addSelection = it },
                )
                if (addSelection.isNotEmpty()) {
                    AddToSheet(
                        songs = addSelection,
                        onAddToQueue = playbackViewModel::addToQueue,
                        onDismiss = { addSelection = emptyList() },
                    )
                }
            }
            composable<SearchRoute> {
                SearchScreen(
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playFromAllSongs(song.id)
                    },
                    onAlbumClick = { navController.navigate(AlbumDetailRoute(it)) },
                    onArtistClick = { navController.navigate(ArtistDetailRoute(it)) },
                    onFolderClick = { navController.navigate(FolderDetailRoute(it)) },
                )
            }
            composable<YouRoute> {
                YouScreen(
                    onAppearance = { navController.navigate(AppearanceSettingsRoute) },
                    onLibrary = { navController.navigate(LibrarySettingsRoute) },
                    onPlayback = { navController.navigate(PlaybackSettingsRoute) },
                    onEqualizer = { navController.navigate(EqualizerRoute) },
                    onAbout = { navController.navigate(AboutRoute) },
                    onStats = { navController.navigate(StatsRoute) },
                )
            }
            composable<StatsRoute> {
                com.resonate.player.ui.stats.StatsScreen(onBack = { navController.popBackStack() })
            }
            composable<AppearanceSettingsRoute> {
                AppearanceSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<LibrarySettingsRoute> {
                LibrarySettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<PlaybackSettingsRoute> {
                PlaybackSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<EqualizerRoute> {
                EqualizerScreen(onBack = { navController.popBackStack() })
            }
            composable<AboutRoute> {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable<AlbumDetailRoute> {
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playAlbum(song.albumId, song.id)
                    },
                )
            }
            composable<ArtistDetailRoute> { entry ->
                val artistId = entry.toRoute<ArtistDetailRoute>().artistId
                ArtistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { navController.navigate(AlbumDetailRoute(it)) },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playArtist(artistId, song.id)
                    },
                )
            }
            composable<GenreDetailRoute> { entry ->
                val genreId = entry.toRoute<GenreDetailRoute>().genreId
                GenreDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playGenre(genreId, song.id)
                    },
                )
            }
            composable<PlaylistDetailRoute> { entry ->
                val playlistId = entry.toRoute<PlaylistDetailRoute>().playlistId
                PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSongClick = { _, index ->
                        beforePlay()
                        playbackViewModel.playPlaylist(playlistId, index)
                    },
                )
            }
            composable<FolderDetailRoute> {
                FolderDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSongClick = { song ->
                        beforePlay()
                        playbackViewModel.playFolder(song.folderId, song.id)
                    },
                )
            }
        }
      }
    }
}

@Composable
private fun ScanBanner() {
    val viewModel: LibraryScanViewModel = hiltViewModel()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scanning = scanState as? ScanState.Scanning ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            color = ResonateTheme.colors.mint,
            trackColor = ResonateTheme.colors.surfaceRaised,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.scan_in_progress, scanning.found),
            style = ResonateTheme.type.caption,
            color = ResonateTheme.colors.muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TabItem(
    tab: TabDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ResonateTheme.colors
    val tint = if (selected) colors.accent else colors.muted
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.activeIcon else tab.icon,
            contentDescription = stringResource(tab.labelRes),
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(tab.labelRes),
            style = ResonateTheme.type.label,
            color = tint,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
