package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.FollowedArtistDoc
import com.example.model.LikedTrackDoc
import com.example.model.ListeningHistoryDoc
import com.example.model.PlaylistDoc
import com.example.model.PlaylistTrackDoc
import com.example.model.SavedAlbumDoc
import com.example.model.Track
import com.example.playback.PlaybackUiState
import com.example.ui.components.LoadingTrackListSkeleton
import com.example.ui.components.PlaylistEditDialog
import com.example.ui.components.TrackRowItem
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassHeader
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.GlassVariant
import com.example.viewmodel.UiState

private enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED("Liked Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    RECENT("Recent")
}

@Composable
fun LibraryScreen(
    playlistsState: UiState<List<PlaylistDoc>>,
    likedTracksState: UiState<List<LikedTrackDoc>>,
    savedAlbumsState: UiState<List<SavedAlbumDoc>>,
    followedArtistsState: UiState<List<FollowedArtistDoc>>,
    historyState: UiState<List<ListeningHistoryDoc>>,
    selectedPlaylistId: String?,
    selectedPlaylistTracksState: UiState<List<PlaylistTrackDoc>>,
    likedKeys: Set<String>,
    playbackState: PlaybackUiState,
    bottomPadding: Dp,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (String, String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String, Int) -> Unit,
    onMovePlaylistTrack: (String, List<PlaylistTrackDoc>, Int, Int) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onToggleLike: (Track) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onToggleSaveAlbum: (Album) -> Unit,
    onToggleFollowArtist: (Artist) -> Unit,
    onSearchArtist: (String) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<PlaylistDoc?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistDoc?>(null) }

    val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    if (selectedPlaylist != null) {
        BackHandler { onSelectPlaylist(null) }
        PlaylistDetailContent(
            playlist = selectedPlaylist,
            tracksState = selectedPlaylistTracksState,
            playbackState = playbackState,
            bottomPadding = bottomPadding,
            onBack = { onSelectPlaylist(null) },
            onRenameClick = { playlistToEdit = selectedPlaylist },
            onDeleteClick = { playlistToDelete = selectedPlaylist },
            onPlayAll = { tracks, shuffle ->
                val playable = tracks.filter { it.isPlayableInApp }
                if (playable.isNotEmpty()) {
                    onPlayTracks(if (shuffle) playable.shuffled() else playable, 0)
                }
            },
            onPlayIndex = { tracks, idx -> onPlayTracks(tracks, idx) },
            onMoveTrack = { docs, from, to -> onMovePlaylistTrack(selectedPlaylist.id, docs, from, to) },
            onRemoveTrack = { entryId, total -> onRemoveTrackFromPlaylist(selectedPlaylist.id, entryId, total) },
            onOpenExternal = onOpenExternal
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            GlassHeader {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("create_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create playlist"
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.entries) { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (activeTab) {
                LibraryTab.PLAYLISTS -> {
                    when (playlistsState) {
                        UiState.Loading -> LoadingTrackListSkeleton(count = 4, modifier = Modifier.padding(top = 12.dp))
                        is UiState.Error -> Text(
                            text = playlistsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                        is UiState.Success -> {
                            if (playlists.isEmpty()) {
                                LibraryEmptyState(
                                    title = "No playlists yet",
                                    subtitle = "Create a playlist to collect full tracks and 30s previews.",
                                    actionLabel = "Create Playlist",
                                    onAction = { showCreateDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(playlists, key = { it.id }) { pl ->
                                        UserPlaylistRow(
                                            playlist = pl,
                                            onClick = { onSelectPlaylist(pl.id) },
                                            onRename = { playlistToEdit = pl },
                                            onDelete = { playlistToDelete = pl }
                                        )
                                    }
                                    item {
                                        Text(
                                            text = "Streaming catalogs require an active network connection; offline audio downloads are not supported under current source licensing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.LIKED -> {
                    when (likedTracksState) {
                        UiState.Loading -> LoadingTrackListSkeleton(count = 5, modifier = Modifier.padding(top = 12.dp))
                        is UiState.Error -> Text(
                            text = likedTracksState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                        is UiState.Success -> {
                            val tracks = likedTracksState.data.map { it.toTrack() }
                            if (tracks.isEmpty()) {
                                LibraryEmptyState(
                                    title = "No liked songs yet",
                                    subtitle = "Tap the heart icon on any track in Home, Explore, or Search."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                                ) {
                                    item {
                                        val playable = tracks.filter { it.isPlayableInApp }
                                        if (playable.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = { onPlayTracks(playable, 0) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Play All (${playable.size})")
                                                }
                                                OutlinedButton(onClick = { onPlayTracks(playable.shuffled(), 0) }) {
                                                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle")
                                                }
                                            }
                                        }
                                    }
                                    itemsIndexed(tracks, key = { _, t -> "liked_${t.key}" }) { idx, track ->
                                        TrackRowItem(
                                            track = track,
                                            isCurrent = playbackState.currentTrack?.key == track.key,
                                            isPlaying = playbackState.isPlaying,
                                            isLiked = true,
                                            onPlayClick = { onPlayTracks(tracks, idx) },
                                            onOpenExternal = { onOpenExternal(track.externalUrl) },
                                            onToggleLike = { onToggleLike(track) },
                                            onMoreClick = { onTrackMore(track) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    when (savedAlbumsState) {
                        UiState.Loading -> LoadingTrackListSkeleton(count = 4, modifier = Modifier.padding(top = 12.dp))
                        is UiState.Error -> Text(
                            text = savedAlbumsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                        is UiState.Success -> {
                            val albums = savedAlbumsState.data.map { it.toAlbum() }
                            if (albums.isEmpty()) {
                                LibraryEmptyState(
                                    title = "No saved albums",
                                    subtitle = "Open any album and tap the bookmark button to save it here."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(albums, key = { "saved_alb_${it.key}" }) { album ->
                                        GlassCard(
                                            variant = GlassVariant.FROSTED,
                                            onClick = { onOpenAlbum(album) },
                                            contentPadding = PaddingValues(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = album.artworkUrl.ifBlank { null },
                                                    contentDescription = album.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                }
                                                IconButton(onClick = { onToggleSaveAlbum(album) }) {
                                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove saved album")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    when (followedArtistsState) {
                        UiState.Loading -> LoadingTrackListSkeleton(count = 4, modifier = Modifier.padding(top = 12.dp))
                        is UiState.Error -> Text(
                            text = followedArtistsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                        is UiState.Success -> {
                            val artists = followedArtistsState.data.map { it.toArtist() }
                            if (artists.isEmpty()) {
                                LibraryEmptyState(
                                    title = "No followed artists",
                                    subtitle = "Tap any artist on Home or Explore to follow them."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(artists, key = { "fol_art_${it.key}" }) { artist ->
                                        GlassCard(
                                            variant = GlassVariant.FROSTED,
                                            onClick = { onSearchArtist(artist.name) },
                                            contentPadding = PaddingValues(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (artist.artworkUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = artist.artworkUrl,
                                                            contentDescription = artist.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.matchParentSize()
                                                        )
                                                    } else {
                                                        Icon(Icons.Rounded.Person, contentDescription = null)
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                                    if (artist.genre.isNotBlank()) {
                                                        Text(artist.genre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                TextButton(onClick = { onToggleFollowArtist(artist) }) {
                                                    Text("Following")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.RECENT -> {
                    when (historyState) {
                        UiState.Loading -> LoadingTrackListSkeleton(count = 5, modifier = Modifier.padding(top = 12.dp))
                        is UiState.Error -> Text(
                            text = historyState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                        is UiState.Success -> {
                            val tracks = historyState.data.map { it.toTrack() }
                            if (tracks.isEmpty()) {
                                LibraryEmptyState(
                                    title = "No listening history yet",
                                    subtitle = "Tracks you play in MasterWave appear here automatically."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                                ) {
                                    itemsIndexed(tracks, key = { _, t -> "lib_hist_${t.key}" }) { idx, track ->
                                        TrackRowItem(
                                            track = track,
                                            isCurrent = playbackState.currentTrack?.key == track.key,
                                            isPlaying = playbackState.isPlaying,
                                            isLiked = likedKeys.contains(track.key),
                                            onPlayClick = { onPlayTracks(tracks, idx) },
                                            onOpenExternal = { onOpenExternal(track.externalUrl) },
                                            onToggleLike = { onToggleLike(track) },
                                            onMoreClick = { onTrackMore(track) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistEditDialog(
            title = "Create Playlist",
            initialName = "",
            initialDescription = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }

    playlistToEdit?.let { pl ->
        PlaylistEditDialog(
            title = "Edit Playlist",
            initialName = pl.name,
            initialDescription = pl.description,
            confirmLabel = "Save",
            onDismiss = { playlistToEdit = null },
            onConfirm = { name, desc ->
                onRenamePlaylist(pl.id, name, desc)
                playlistToEdit = null
            }
        )
    }

    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete \"${pl.name}\"?") },
            text = { Text("This removes the playlist and all its saved track entries from your library.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(pl.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserPlaylistRow(
    playlist: PlaylistDoc,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        variant = GlassVariant.FROSTED,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = playlist.artworkUrl,
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.trackCount} tracks" +
                        if (playlist.description.isNotBlank()) " • ${playlist.description}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    playlist: PlaylistDoc,
    tracksState: UiState<List<PlaylistTrackDoc>>,
    playbackState: PlaybackUiState,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayIndex: (List<Track>, Int) -> Unit,
    onMoveTrack: (List<PlaylistTrackDoc>, Int, Int) -> Unit,
    onRemoveTrack: (String, Int) -> Unit,
    onOpenExternal: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Library")
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = onRenameClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist")
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete playlist")
                }
            }
        }

        when (tracksState) {
            UiState.Loading -> LoadingTrackListSkeleton(count = 5, modifier = Modifier.padding(top = 12.dp))
            is UiState.Error -> Text(
                text = tracksState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(20.dp)
            )
            is UiState.Success -> {
                val entries = tracksState.data
                val tracks = entries.map { it.toTrack() }
                if (entries.isEmpty()) {
                    LibraryEmptyState(
                        title = "This playlist is empty",
                        subtitle = "Tap the menu icon on any song to add it to \"${playlist.name}\"."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onPlayAll(tracks, false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play All")
                                }
                                OutlinedButton(onClick = { onPlayAll(tracks, true) }) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle")
                                }
                            }
                        }

                        itemsIndexed(entries, key = { _, e -> e.id }) { idx, entry ->
                            val track = entry.toTrack()
                            val isCurrent = playbackState.currentTrack?.key == track.key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (track.isPlayableInApp) onPlayIndex(tracks, idx)
                                        else onOpenExternal(track.externalUrl)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl.ifBlank { null },
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveTrack(entries, idx, idx - 1) },
                                    enabled = idx > 0
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { onMoveTrack(entries, idx, idx + 1) },
                                    enabled = idx < entries.lastIndex
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                                }
                                IconButton(
                                    onClick = { onRemoveTrack(entry.id, entries.size) }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove from playlist")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_studio),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 200.dp, height = 140.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
