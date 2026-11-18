package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.remote.CatalogFeed
import com.example.model.Album
import com.example.model.Artist
import com.example.model.CuratedPlaylist
import com.example.model.GenreMood
import com.example.model.Track
import com.example.playback.PlaybackUiState
import com.example.ui.components.AlbumSquareCard
import com.example.ui.components.ArtistBubbleCard
import com.example.ui.components.LoadingTrackListSkeleton
import com.example.ui.components.PlaylistSquareCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRowItem
import com.example.ui.glass.GlassHeader
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    genresAndMoods: List<GenreMood>,
    catalogState: UiState<CatalogFeed>,
    selectedGenre: GenreMood?,
    genreTracksState: UiState<List<Track>>,
    likedKeys: Set<String>,
    followedArtistKeys: Set<String>,
    playbackState: PlaybackUiState,
    bottomPadding: Dp,
    onGenreClick: (GenreMood) -> Unit,
    onDismissGenre: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onOpenPlaylist: (CuratedPlaylist) -> Unit,
    onToggleLike: (Track) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = genresAndMoods.filter { !it.isMood }
    val moods = genresAndMoods.filter { it.isMood }

    Column(modifier = modifier.fillMaxSize()) {
        GlassHeader {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "Genres, Moods & Releases",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Music Genres Grid (2 columns)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Music Genres",
                        subtitle = "Tap any genre for real streams & catalog previews"
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        genres.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { genre ->
                                    GenreTileCard(
                                        genre = genre,
                                        onClick = { onGenreClick(genre) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Mood-based Playlists
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Moods & Atmospheres",
                        subtitle = "Curated soundscapes for focus, drive & energy"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(moods, key = { it.id }) { mood ->
                            GenreTileCard(
                                genre = mood,
                                onClick = { onGenreClick(mood) },
                                modifier = Modifier.size(width = 176.dp, height = 100.dp)
                            )
                        }
                    }
                }
            }

            when (catalogState) {
                UiState.Loading -> item { LoadingTrackListSkeleton(count = 4) }
                is UiState.Error -> item {
                    Text(
                        text = catalogState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                is UiState.Success -> {
                    val feed = catalogState.data

                    if (feed.newReleasesTracks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "New Releases",
                                subtitle = "Official 30s previews & fresh catalog singles"
                            )
                        }
                        itemsIndexed(
                            feed.newReleasesTracks.take(8),
                            key = { _, t -> "new_${t.key}" }
                        ) { idx, track ->
                            TrackRowItem(
                                track = track,
                                isCurrent = playbackState.currentTrack?.key == track.key,
                                isPlaying = playbackState.isPlaying,
                                isLiked = likedKeys.contains(track.key),
                                onPlayClick = { onPlayTracks(feed.newReleasesTracks, idx) },
                                onOpenExternal = { onOpenExternal(track.externalUrl) },
                                onToggleLike = { onToggleLike(track) },
                                onMoreClick = { onTrackMore(track) }
                            )
                        }
                    }

                    if (feed.recommendedAlbums.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(title = "Popular Albums")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(feed.recommendedAlbums, key = { "exp_alb_${it.key}" }) { album ->
                                        AlbumSquareCard(album = album, onClick = { onOpenAlbum(album) })
                                    }
                                }
                            }
                        }
                    }

                    if (feed.popularArtists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(title = "Popular Artists")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(feed.popularArtists, key = { "exp_art_${it.key}" }) { artist ->
                                        ArtistBubbleCard(
                                            artist = artist,
                                            isFollowed = followedArtistKeys.contains(artist.key),
                                            onClick = { onArtistClick(artist) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (feed.featuredPlaylists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(title = "Recommended Collections")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(feed.featuredPlaylists, key = { "exp_pl_${it.key}" }) { pl ->
                                        PlaylistSquareCard(playlist = pl, onClick = { onOpenPlaylist(pl) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedGenre != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissGenre,
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedGenre.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = selectedGenre.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val loadedTracks = (genreTracksState as? UiState.Success)?.data.orEmpty()
                    val playable = loadedTracks.filter { it.isPlayableInApp }
                    if (playable.isNotEmpty()) {
                        Button(onClick = { onPlayTracks(playable, 0) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Play")
                        }
                    }
                }

                when (genreTracksState) {
                    UiState.Loading -> LoadingTrackListSkeleton(count = 5, modifier = Modifier.padding(vertical = 12.dp))
                    is UiState.Error -> Text(
                        text = genreTracksState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(20.dp)
                    )
                    is UiState.Success -> {
                        val tracks = genreTracksState.data
                        LazyColumn(modifier = Modifier.height(340.dp)) {
                            itemsIndexed(tracks, key = { _, t -> "genre_${t.key}" }) { idx, track ->
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

@Composable
private fun GenreTileCard(
    genre: GenreMood,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(genre.accentHex)
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.28f),
                        accent.copy(alpha = 0.12f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = genre.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = genre.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
