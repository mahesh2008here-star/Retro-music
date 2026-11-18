package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.CuratedPlaylist
import com.example.model.SearchResults
import com.example.model.Track
import com.example.playback.PlaybackUiState
import com.example.ui.components.AlbumSquareCard
import com.example.ui.components.ArtistBubbleCard
import com.example.ui.components.LoadingTrackListSkeleton
import com.example.ui.components.PlaylistSquareCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRowItem
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.GlassVariant
import com.example.viewmodel.UiState

private enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    recentSearches: List<String>,
    suggestions: List<String>,
    searchState: UiState<SearchResults>,
    likedKeys: Set<String>,
    followedArtistKeys: Set<String>,
    playbackState: PlaybackUiState,
    bottomPadding: Dp,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onOpenPlaylist: (CuratedPlaylist) -> Unit,
    onToggleLike: (Track) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Liquid Glass Search Input Bar
        GlassSurface(
            variant = GlassVariant.FROSTED,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search songs, albums, artists, moods…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onSubmitSearch(query)
                            keyboard?.hide()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            }
        }

        if (query.isBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onClearRecent) {
                                    Text("Clear all")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { term ->
                                    InputChip(
                                        selected = false,
                                        onClick = { onSubmitSearch(term) },
                                        label = { Text(term) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Remove $term",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { onRemoveRecent(term) }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Suggested Queries",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { term ->
                                SuggestionChip(
                                    onClick = { onSubmitSearch(term) },
                                    label = { Text(term) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            when (searchState) {
                UiState.Loading -> {
                    LoadingTrackListSkeleton(count = 6, modifier = Modifier.padding(top = 16.dp))
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = searchState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onSubmitSearch(query) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
                is UiState.Success -> {
                    val res = searchState.data
                    val noResults = res.songs.isEmpty() && res.albums.isEmpty() &&
                        res.artists.isEmpty() && res.playlists.isEmpty()

                    if (noResults) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_studio),
                                contentDescription = "No matching results",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 210.dp, height = 148.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No matches for \"$query\"",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try another artist, album title, or genre keyword.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 10.dp, bottom = bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SONGS) &&
                                res.songs.isNotEmpty()
                            ) {
                                item { SectionHeader(title = "Songs (${res.songs.size})") }
                                val list = if (selectedFilter == SearchFilter.ALL) res.songs.take(8) else res.songs
                                itemsIndexed(list, key = { _, t -> "srch_t_${t.key}" }) { idx, track ->
                                    TrackRowItem(
                                        track = track,
                                        isCurrent = playbackState.currentTrack?.key == track.key,
                                        isPlaying = playbackState.isPlaying,
                                        isLiked = likedKeys.contains(track.key),
                                        onPlayClick = { onPlayTracks(res.songs, idx) },
                                        onOpenExternal = { onOpenExternal(track.externalUrl) },
                                        onToggleLike = { onToggleLike(track) },
                                        onMoreClick = { onTrackMore(track) }
                                    )
                                }
                            }

                            if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ALBUMS) &&
                                res.albums.isNotEmpty()
                            ) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SectionHeader(title = "Albums (${res.albums.size})")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(res.albums, key = { "srch_a_${it.key}" }) { album ->
                                                AlbumSquareCard(album = album, onClick = { onOpenAlbum(album) })
                                            }
                                        }
                                    }
                                }
                            }

                            if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ARTISTS) &&
                                res.artists.isNotEmpty()
                            ) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SectionHeader(title = "Artists (${res.artists.size})")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(res.artists, key = { "srch_ar_${it.key}" }) { artist ->
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

                            if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.PLAYLISTS) &&
                                res.playlists.isNotEmpty()
                            ) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SectionHeader(title = "Playlists (${res.playlists.size})")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(res.playlists, key = { "srch_p_${it.key}" }) { pl ->
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
        }
    }
}
