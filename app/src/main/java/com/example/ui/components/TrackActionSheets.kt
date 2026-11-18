package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Album
import com.example.model.PlaylistDoc
import com.example.model.Track
import com.example.viewmodel.UiState

fun openExternalMusicUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    track: Track,
    isLiked: Boolean,
    playlists: List<PlaylistDoc>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (PlaylistDoc) -> Unit,
    onCreatePlaylistWithTrack: (String) -> Unit,
    onOpenExternal: () -> Unit
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    model = track.artworkUrl.ifBlank { null },
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.source.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    CapabilityBadge(capability = track.capability)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (!showPlaylistPicker) {
                if (track.isPlayableInApp) {
                    ListItem(
                        headlineContent = { Text("Play next") },
                        leadingContent = { Icon(Icons.Rounded.QueuePlayNext, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onPlayNext()
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Add to queue") },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onAddToQueue()
                            onDismiss()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs") },
                    leadingContent = {
                        Icon(
                            if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onToggleLike()
                        onDismiss()
                    }
                )

                ListItem(
                    headlineContent = { Text("Add to playlist") },
                    leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showPlaylistPicker = true }
                )

                if (track.externalUrl.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Open in ${track.source.label}") },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onOpenExternal()
                            onDismiss()
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose playlist",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet. Create one to save this track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.trackCount} tracks") },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    onAddToPlaylist(playlist)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        PlaylistEditDialog(
            title = "New Playlist",
            initialName = "",
            initialDescription = "",
            confirmLabel = "Create & Add",
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, _ ->
                onCreatePlaylistWithTrack(name)
                showCreatePlaylistDialog = false
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailSheet(
    album: Album,
    tracksState: UiState<List<Track>>,
    isSaved: Boolean,
    likedKeys: Set<String>,
    currentTrackKey: String?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onToggleLike: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onOpenExternal: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    model = album.artworkUrl.ifBlank { null },
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val meta = listOfNotNull(
                        album.releaseYear.takeIf { it.isNotBlank() },
                        album.genre.takeIf { it.isNotBlank() },
                        album.source.label
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val tracks = (tracksState as? UiState.Success)?.data.orEmpty()
            val playableTracks = tracks.filter { it.isPlayableInApp }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (playableTracks.isNotEmpty()) {
                    Button(
                        onClick = { onPlayTracks(playableTracks, 0) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play Previews")
                    }
                    OutlinedButton(
                        onClick = { onPlayTracks(playableTracks.shuffled(), 0) }
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(18.dp))
                    }
                } else if (album.externalUrl.isNotBlank()) {
                    Button(
                        onClick = { onOpenExternal(album.externalUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open on ${album.source.label}")
                    }
                }

                OutlinedButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save Album",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            when (tracksState) {
                UiState.Loading -> LoadingTrackListSkeleton(count = 4, modifier = Modifier.padding(vertical = 12.dp))
                is UiState.Error -> Text(
                    text = tracksState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(20.dp)
                )
                is UiState.Success -> {
                    if (tracks.isEmpty()) {
                        Text(
                            text = "Tracklist unavailable from catalog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(tracks, key = { it.key }) { track ->
                                TrackRowItem(
                                    track = track,
                                    isCurrent = track.key == currentTrackKey,
                                    isPlaying = isPlaying,
                                    isLiked = likedKeys.contains(track.key),
                                    onPlayClick = { onPlayTracks(tracks, tracks.indexOf(track)) },
                                    onOpenExternal = { onOpenExternal(track.externalUrl) },
                                    onToggleLike = { onToggleLike(track) },
                                    onMoreClick = { onMoreTrackClick(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 240) description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), description.trim()) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_playlist_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
