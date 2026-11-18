package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.data.firebase.LibraryRepository
import com.example.model.Artist
import com.example.model.CuratedPlaylist
import com.example.model.Track
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AlbumDetailSheet
import com.example.ui.components.TrackOptionsSheet
import com.example.ui.components.TrackRowItem
import com.example.ui.components.openExternalMusicUrl
import com.example.ui.glass.DockDestination
import com.example.ui.glass.GlassBackdropSource
import com.example.ui.glass.GlassDock
import com.example.ui.glass.GlassMiniPlayer
import com.example.ui.glass.GlassProvider
import com.example.ui.glass.LocalGlassContext
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MasterWaveTheme
import com.example.viewmodel.LibraryViewModel
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private object NavRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val SEARCH = "search"
    const val LIBRARY = "library"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            val preferences by musicViewModel.preferencesState.collectAsStateWithLifecycle()
            val palette by musicViewModel.artworkPalette.collectAsStateWithLifecycle()

            GlassProvider(
                themeMode = preferences.themeMode,
                glassQualityMode = preferences.glassQuality,
                accentColor = palette.vibrant,
                reducedMotion = preferences.reducedMotion
            ) {
                MasterWaveTheme(
                    themeMode = preferences.themeMode,
                    accentColor = palette.vibrant
                ) {
                    MasterWaveRoot(musicViewModel = musicViewModel)
                }
            }
        }
    }
}

@Composable
fun MasterWaveRoot(
    musicViewModel: MusicViewModel,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                // AuthStateListener updates currentUser reactively
            }
        )
    } else {
        key(user.uid) {
            MasterWaveAuthenticatedShell(
                user = user,
                musicViewModel = musicViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MasterWaveAuthenticatedShell(
    user: FirebaseUser,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val libraryViewModel: LibraryViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LibraryViewModel(
                    repository = LibraryRepository(db),
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "MasterWave Listener",
                    userEmail = user.email,
                    userPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    DisposableEffect(libraryViewModel) {
        musicViewModel.attachHistoryRecorder { track ->
            libraryViewModel.recordPlay(track)
        }
        onDispose {
            musicViewModel.attachHistoryRecorder {}
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Playback continues regardless of notification permission decision
    }
    var requestedNotificationPerm by rememberSaveable { mutableStateOf(false) }
    fun ensurePlaybackNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requestedNotificationPerm) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestedNotificationPerm = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val preferences by musicViewModel.preferencesState.collectAsStateWithLifecycle()
    val playbackState by musicViewModel.playbackState.collectAsStateWithLifecycle()
    val catalogState by musicViewModel.catalogState.collectAsStateWithLifecycle()
    val searchQuery by musicViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by musicViewModel.searchState.collectAsStateWithLifecycle()
    val selectedGenre by musicViewModel.selectedGenre.collectAsStateWithLifecycle()
    val genreTracksState by musicViewModel.genreTracksState.collectAsStateWithLifecycle()
    val selectedAlbum by musicViewModel.selectedAlbum.collectAsStateWithLifecycle()
    val albumTracksState by musicViewModel.albumTracksState.collectAsStateWithLifecycle()
    val artworkPalette by musicViewModel.artworkPalette.collectAsStateWithLifecycle()

    val playlistsState by libraryViewModel.playlistsState.collectAsStateWithLifecycle()
    val likedTracksState by libraryViewModel.likedTracksState.collectAsStateWithLifecycle()
    val savedAlbumsState by libraryViewModel.savedAlbumsState.collectAsStateWithLifecycle()
    val followedArtistsState by libraryViewModel.followedArtistsState.collectAsStateWithLifecycle()
    val historyState by libraryViewModel.listeningHistoryState.collectAsStateWithLifecycle()
    val selectedPlaylistId by libraryViewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val selectedPlaylistTracksState by libraryViewModel.selectedPlaylistTracksState.collectAsStateWithLifecycle()

    val likedKeys by libraryViewModel.likedTrackKeys.collectAsStateWithLifecycle()
    val savedAlbumKeys by libraryViewModel.savedAlbumKeys.collectAsStateWithLifecycle()
    val followedArtistKeys by libraryViewModel.followedArtistKeys.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(NavRoutes.HOME) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }

    var optionsTrack by remember { mutableStateOf<Track?>(null) }
    var inspectedPlaylist by remember { mutableStateOf<CuratedPlaylist?>(null) }
    var inspectedArtist by remember { mutableStateOf<Artist?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(libraryViewModel) {
        libraryViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(playbackState.errorMessage) {
        val err = playbackState.errorMessage
        if (!err.isNullOrBlank() && !showNowPlaying) {
            snackbarHostState.showSnackbar(err)
            musicViewModel.playbackManager.clearError()
        }
    }

    val destinations = remember {
        listOf(
            DockDestination(NavRoutes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home, "dock_home"),
            DockDestination(NavRoutes.EXPLORE, "Explore", Icons.Outlined.Explore, Icons.Rounded.Explore, "dock_explore"),
            DockDestination(NavRoutes.SEARCH, "Search", Icons.Outlined.Search, Icons.Rounded.Search, "dock_search"),
            DockDestination(NavRoutes.LIBRARY, "Library", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic, "dock_library")
        )
    }

    val hasActiveTrack = playbackState.currentTrack != null
    val contentBottomPadding = if (hasActiveTrack) 186.dp else 112.dp
    val glass = LocalGlassContext.current

    fun playTracksAction(tracks: List<Track>, index: Int) {
        ensurePlaybackNotificationPermission()
        musicViewModel.playbackManager.playQueue(tracks, index)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (hasActiveTrack) 154.dp else 82.dp)
            )
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Live Backdrop Source recorded for floating Liquid Glass Dock, MiniPlayer & Header
            GlassBackdropSource(modifier = Modifier.fillMaxSize()) {
                // Subtle adaptive ambient glow matching current track or MasterWave accent
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    glass.accentColor.copy(alpha = if (glass.isDark) 0.14f else 0.08f),
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                AnimatedContent(
                    targetState = currentTab,
                    label = "mainTabTransition"
                ) { tab ->
                    when (tab) {
                        NavRoutes.HOME -> HomeScreen(
                            userName = user.displayName ?: "Listener",
                            userPhotoUrl = user.photoUrl?.toString(),
                            catalogState = catalogState,
                            historyState = historyState,
                            likedKeys = likedKeys,
                            followedArtistKeys = followedArtistKeys,
                            playbackState = playbackState,
                            bottomPadding = contentBottomPadding,
                            onSearchClick = { currentTab = NavRoutes.SEARCH },
                            onSettingsClick = { showSettings = true },
                            onRefreshCatalog = { musicViewModel.loadCatalog(forceRefresh = true) },
                            onPlayTracks = ::playTracksAction,
                            onOpenNowPlaying = { showNowPlaying = true },
                            onOpenAlbum = { musicViewModel.openAlbum(it) },
                            onArtistClick = { inspectedArtist = it },
                            onOpenPlaylist = { inspectedPlaylist = it },
                            onToggleLike = { libraryViewModel.toggleLike(it) },
                            onTrackMore = { optionsTrack = it },
                            onOpenExternal = { openExternalMusicUrl(context, it) }
                        )

                        NavRoutes.EXPLORE -> ExploreScreen(
                            genresAndMoods = musicViewModel.catalogRepository.genresAndMoods,
                            catalogState = catalogState,
                            selectedGenre = selectedGenre,
                            genreTracksState = genreTracksState,
                            likedKeys = likedKeys,
                            followedArtistKeys = followedArtistKeys,
                            playbackState = playbackState,
                            bottomPadding = contentBottomPadding,
                            onGenreClick = { musicViewModel.openGenre(it) },
                            onDismissGenre = { musicViewModel.closeGenre() },
                            onPlayTracks = ::playTracksAction,
                            onOpenAlbum = { musicViewModel.openAlbum(it) },
                            onArtistClick = { inspectedArtist = it },
                            onOpenPlaylist = { inspectedPlaylist = it },
                            onToggleLike = { libraryViewModel.toggleLike(it) },
                            onTrackMore = { optionsTrack = it },
                            onOpenExternal = { openExternalMusicUrl(context, it) }
                        )

                        NavRoutes.SEARCH -> SearchScreen(
                            query = searchQuery,
                            recentSearches = preferences.recentSearches,
                            suggestions = musicViewModel.catalogRepository.quickSuggestions,
                            searchState = searchState,
                            likedKeys = likedKeys,
                            followedArtistKeys = followedArtistKeys,
                            playbackState = playbackState,
                            bottomPadding = contentBottomPadding,
                            onQueryChange = { musicViewModel.updateSearchQuery(it) },
                            onSubmitSearch = { musicViewModel.submitSearch(it) },
                            onRemoveRecent = { musicViewModel.removeRecentSearch(it) },
                            onClearRecent = { musicViewModel.clearRecentSearches() },
                            onPlayTracks = ::playTracksAction,
                            onOpenAlbum = { musicViewModel.openAlbum(it) },
                            onArtistClick = { inspectedArtist = it },
                            onOpenPlaylist = { inspectedPlaylist = it },
                            onToggleLike = { libraryViewModel.toggleLike(it) },
                            onTrackMore = { optionsTrack = it },
                            onOpenExternal = { openExternalMusicUrl(context, it) }
                        )

                        NavRoutes.LIBRARY -> LibraryScreen(
                            playlistsState = playlistsState,
                            likedTracksState = likedTracksState,
                            savedAlbumsState = savedAlbumsState,
                            followedArtistsState = followedArtistsState,
                            historyState = historyState,
                            selectedPlaylistId = selectedPlaylistId,
                            selectedPlaylistTracksState = selectedPlaylistTracksState,
                            likedKeys = likedKeys,
                            playbackState = playbackState,
                            bottomPadding = contentBottomPadding,
                            onSelectPlaylist = { libraryViewModel.selectPlaylist(it) },
                            onCreatePlaylist = { name, desc -> libraryViewModel.createPlaylist(name, desc) },
                            onRenamePlaylist = { id, name, desc -> libraryViewModel.renamePlaylist(id, name, desc) },
                            onDeletePlaylist = { libraryViewModel.deletePlaylist(it) },
                            onRemoveTrackFromPlaylist = { plId, entryId, total ->
                                libraryViewModel.removeTrackFromPlaylist(plId, entryId, total)
                            },
                            onMovePlaylistTrack = { plId, list, from, to ->
                                libraryViewModel.movePlaylistTrack(plId, list, from, to)
                            },
                            onPlayTracks = ::playTracksAction,
                            onToggleLike = { libraryViewModel.toggleLike(it) },
                            onOpenAlbum = { musicViewModel.openAlbum(it) },
                            onToggleSaveAlbum = { libraryViewModel.toggleSaveAlbum(it) },
                            onToggleFollowArtist = { libraryViewModel.toggleFollowArtist(it) },
                            onSearchArtist = { artistName ->
                                musicViewModel.submitSearch(artistName)
                                currentTab = NavRoutes.SEARCH
                            },
                            onTrackMore = { optionsTrack = it },
                            onOpenExternal = { openExternalMusicUrl(context, it) }
                        )
                    }
                }
            }

            // 2. Signature Floating Mini-Player + Liquid Glass Navigation Dock
            if (!showSettings && !showNowPlaying) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = hasActiveTrack,
                        enter = slideInVertically { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        GlassMiniPlayer(
                            playbackState = playbackState,
                            onOpenNowPlaying = { showNowPlaying = true },
                            onTogglePlayPause = { musicViewModel.playbackManager.togglePlayPause() },
                            onNext = { musicViewModel.playbackManager.next() }
                        )
                    }

                    GlassDock(
                        destinations = destinations,
                        currentRoute = currentTab,
                        onNavigate = { route ->
                            if (currentTab != route) {
                                currentTab = route
                            }
                        }
                    )
                }
            }

            // 3. Settings Screen Overlay
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically { it / 8 } + fadeIn(),
                exit = slideOutVertically { it / 8 } + fadeOut()
            ) {
                BackHandler { showSettings = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SettingsScreen(
                        userName = user.displayName ?: "MasterWave Listener",
                        userEmail = user.email,
                        userPhotoUrl = user.photoUrl?.toString(),
                        preferences = preferences,
                        onBack = { showSettings = false },
                        onSelectTheme = { musicViewModel.setThemeMode(it) },
                        onSelectGlassQuality = { musicViewModel.setGlassQuality(it) },
                        onSelectAudioQuality = {
                            musicViewModel.setAudioQuality(it)
                            libraryViewModel.syncAudioQuality(it)
                        },
                        onToggleReducedMotion = { musicViewModel.setReducedMotion(it) },
                        onToggleDataSaver = { musicViewModel.setDataSaver(it) },
                        onClearCache = { musicViewModel.clearAllCaches() },
                        onSignOut = {
                            musicViewModel.playbackManager.clearQueue()
                            showSettings = false
                            signOutUser(credentialManager, scope)
                        }
                    )
                }
            }

            // 4. Full-Screen Now Playing Overlay
            AnimatedVisibility(
                visible = showNowPlaying,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val activeTrack = playbackState.currentTrack
                NowPlayingScreen(
                    playbackState = playbackState,
                    palette = artworkPalette,
                    isLiked = activeTrack != null && likedKeys.contains(activeTrack.key),
                    onDismiss = { showNowPlaying = false },
                    onTogglePlayPause = { musicViewModel.playbackManager.togglePlayPause() },
                    onNext = { musicViewModel.playbackManager.next() },
                    onPrevious = { musicViewModel.playbackManager.previous() },
                    onSeekTo = { musicViewModel.playbackManager.seekTo(it) },
                    onVolumeChange = { musicViewModel.playbackManager.setVolume(it) },
                    onToggleShuffle = { musicViewModel.playbackManager.toggleShuffle() },
                    onCycleRepeat = { musicViewModel.playbackManager.cycleRepeatMode() },
                    onToggleLike = { activeTrack?.let { libraryViewModel.toggleLike(it) } },
                    onOpenQueue = { showQueue = true },
                    onMoreClick = { activeTrack?.let { optionsTrack = it } },
                    onOpenExternal = { openExternalMusicUrl(context, it) },
                    onDismissError = { musicViewModel.playbackManager.clearError() }
                )
            }
        }
    }

    // 5. Queue Bottom Sheet
    if (showQueue) {
        QueueSheet(
            playbackState = playbackState,
            onDismiss = { showQueue = false },
            onPlayIndex = { musicViewModel.playbackManager.playQueueIndex(it) },
            onMoveItem = { from, to -> musicViewModel.playbackManager.moveQueueItem(from, to) },
            onRemoveItem = { musicViewModel.playbackManager.removeFromQueue(it) },
            onClearQueue = { musicViewModel.playbackManager.clearQueue() }
        )
    }

    // 6. Track Options Sheet
    optionsTrack?.let { track ->
        val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()
        TrackOptionsSheet(
            track = track,
            isLiked = likedKeys.contains(track.key),
            playlists = playlists,
            onDismiss = { optionsTrack = null },
            onPlayNext = {
                ensurePlaybackNotificationPermission()
                musicViewModel.playbackManager.playNextInQueue(track)
            },
            onAddToQueue = {
                ensurePlaybackNotificationPermission()
                musicViewModel.playbackManager.addToQueue(track)
            },
            onToggleLike = { libraryViewModel.toggleLike(track) },
            onAddToPlaylist = { pl -> libraryViewModel.addTrackToPlaylist(pl, track) },
            onCreatePlaylistWithTrack = { name -> libraryViewModel.createPlaylist(name, "", track) },
            onOpenExternal = { openExternalMusicUrl(context, track.externalUrl) }
        )
    }

    // 7. Album Detail Sheet
    selectedAlbum?.let { album ->
        AlbumDetailSheet(
            album = album,
            tracksState = albumTracksState,
            isSaved = savedAlbumKeys.contains(album.key),
            likedKeys = likedKeys,
            currentTrackKey = playbackState.currentTrack?.key,
            isPlaying = playbackState.isPlaying,
            onDismiss = { musicViewModel.closeAlbum() },
            onToggleSave = { libraryViewModel.toggleSaveAlbum(album) },
            onPlayTracks = ::playTracksAction,
            onToggleLike = { libraryViewModel.toggleLike(it) },
            onMoreTrackClick = { optionsTrack = it },
            onOpenExternal = { openExternalMusicUrl(context, it) }
        )
    }

    // 8. Curated Playlist Detail Sheet
    inspectedPlaylist?.let { playlist ->
        ModalBottomSheet(
            onDismissRequest = { inspectedPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = playlist.artworkUrl.ifBlank { null },
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(playlist.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
                val playable = playlist.tracks.filter { it.isPlayableInApp }
                if (playable.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { playTracksAction(playable, 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play All (${playable.size})")
                        }
                        OutlinedButton(onClick = { playTracksAction(playable.shuffled(), 0) }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    itemsIndexed(playlist.tracks, key = { _, t -> "cur_item_${t.key}" }) { idx, track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = playbackState.currentTrack?.key == track.key,
                            isPlaying = playbackState.isPlaying,
                            isLiked = likedKeys.contains(track.key),
                            onPlayClick = { playTracksAction(playlist.tracks, idx) },
                            onOpenExternal = { openExternalMusicUrl(context, track.externalUrl) },
                            onToggleLike = { libraryViewModel.toggleLike(track) },
                            onMoreClick = { optionsTrack = track }
                        )
                    }
                }
            }
        }
    }

    // 9. Artist Quick Sheet
    inspectedArtist?.let { artist ->
        val isFollowed = followedArtistKeys.contains(artist.key)
        ModalBottomSheet(
            onDismissRequest = { inspectedArtist = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artist.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = artist.artworkUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleLarge)
                        if (artist.genre.isNotBlank()) {
                            Text(artist.genre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (artist.bio.isNotBlank()) {
                    Text(
                        text = artist.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { libraryViewModel.toggleFollowArtist(artist) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isFollowed) "Following" else "Follow Artist")
                    }
                    OutlinedButton(
                        onClick = {
                            musicViewModel.submitSearch(artist.name)
                            inspectedArtist = null
                            currentTab = NavRoutes.SEARCH
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Find Tracks")
                    }
                }
                if (artist.externalUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { openExternalMusicUrl(context, artist.externalUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open on ${artist.source.label}")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
