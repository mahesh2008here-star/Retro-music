package com.example.ui.screens

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.remote.CatalogFeed
import com.example.model.Album
import com.example.model.Artist
import com.example.model.CuratedPlaylist
import com.example.model.ListeningHistoryDoc
import com.example.model.Track
import com.example.playback.PlaybackUiState
import com.example.ui.components.AlbumSquareCard
import com.example.ui.components.ArtistBubbleCard
import com.example.ui.components.LoadingTrackListSkeleton
import com.example.ui.components.MasterWaveWordmark
import com.example.ui.components.PlaylistSquareCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRowItem
import com.example.ui.glass.GlassButton
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassHeader
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.GlassVariant
import com.example.ui.glass.LocalGlassContext
import com.example.viewmodel.UiState
import java.util.Calendar

@Composable
fun HomeScreen(
    userName: String,
    userPhotoUrl: String?,
    catalogState: UiState<CatalogFeed>,
    historyState: UiState<List<ListeningHistoryDoc>>,
    likedKeys: Set<String>,
    followedArtistKeys: Set<String>,
    playbackState: PlaybackUiState,
    bottomPadding: Dp,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onOpenPlaylist: (CuratedPlaylist) -> Unit,
    onToggleLike: (Track) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night session"
        }
    }
    val glass = LocalGlassContext.current
    val historyTracks = (historyState as? UiState.Success)?.data?.map { it.toTrack() }.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        GlassHeader {
            MasterWaveWordmark(accent = glass.accentColor)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search music"
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_settings_button")
                ) {
                    if (!userPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile and settings",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Profile and settings"
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Personalized Greeting
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "$greeting, ${userName.substringBefore(" ").ifBlank { "Listener" }}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Curated acoustic streams and lossless-grade glass ergonomics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Continue Listening banner (when an active track exists)
            playbackState.currentTrack?.let { active ->
                item {
                    GlassCard(
                        variant = GlassVariant.TINTED,
                        cornerRadius = 22.dp,
                        onClick = onOpenNowPlaying,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = active.artworkUrl.ifBlank { null },
                                contentDescription = active.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CONTINUE LISTENING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = active.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = active.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Open Now Playing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // 3. Catalog State
            when (catalogState) {
                UiState.Loading -> {
                    item {
                        LoadingTrackListSkeleton(count = 6)
                    }
                }
                is UiState.Error -> {
                    item {
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            variant = GlassVariant.FROSTED
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = catalogState.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = onRefreshCatalog) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val feed = catalogState.data

                    if (feed.providerWarnings.isNotEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = feed.providerWarnings.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // 4. Featured Music Carousel
                    val featuredItems = feed.featuredPlaylists.take(5)
                    if (featuredItems.isNotEmpty()) {
                        item {
                            val pagerState = rememberPagerState(pageCount = { featuredItems.size })
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Featured Waves",
                                    subtitle = "High-clarity full-length streams ready to play"
                                )
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    pageSpacing = 14.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) { page ->
                                    val playlist = featuredItems[page]
                                    FeaturedHeroCard(
                                        playlist = playlist,
                                        onClick = { onOpenPlaylist(playlist) },
                                        onPlayClick = {
                                            if (playlist.tracks.isNotEmpty()) {
                                                onPlayTracks(playlist.tracks, 0)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Recently Played
                    if (historyTracks.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Recently Played",
                                    subtitle = "Synced across your MasterWave sessions"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(historyTracks.take(12), key = { _, t -> "hist_${t.key}" }) { idx, track ->
                                        CompactTrackTile(
                                            track = track,
                                            onClick = {
                                                if (track.isPlayableInApp) onPlayTracks(historyTracks, idx)
                                                else onOpenExternal(track.externalUrl)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Made For You (Curated Mixes)
                    val madeForYou = buildMadeForYouMixes(feed)
                    if (madeForYou.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Made For You",
                                    subtitle = "Tailored acoustic blends & stream mixes"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(madeForYou, key = { it.key }) { pl ->
                                        PlaylistSquareCard(
                                            playlist = pl,
                                            onClick = { onOpenPlaylist(pl) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Recommended Albums
                    if (feed.recommendedAlbums.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Recommended Albums",
                                    subtitle = "Tap any album to inspect tracklist & 30s previews"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(feed.recommendedAlbums, key = { it.key }) { album ->
                                        AlbumSquareCard(
                                            album = album,
                                            onClick = { onOpenAlbum(album) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Popular Artists
                    if (feed.popularArtists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Popular Artists",
                                    subtitle = "Tap to follow or explore catalog"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(feed.popularArtists, key = { it.key }) { artist ->
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

                    // 9. Trending Music (Full Track Streams)
                    if (feed.trendingFullTracks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Trending Full Streams",
                                subtitle = "Verified Audius independent & CC catalog",
                                actionLabel = "Play All",
                                onActionClick = { onPlayTracks(feed.trendingFullTracks, 0) }
                            )
                        }
                        itemsIndexed(
                            feed.trendingFullTracks.take(8),
                            key = { _, t -> "trend_${t.key}" }
                        ) { idx, track ->
                            TrackRowItem(
                                track = track,
                                isCurrent = playbackState.currentTrack?.key == track.key,
                                isPlaying = playbackState.isPlaying,
                                isLiked = likedKeys.contains(track.key),
                                onPlayClick = { onPlayTracks(feed.trendingFullTracks, idx) },
                                onOpenExternal = { onOpenExternal(track.externalUrl) },
                                onToggleLike = { onToggleLike(track) },
                                onMoreClick = { onTrackMore(track) }
                            )
                        }
                    }

                    // 10. Curated Playlists
                    if (feed.featuredPlaylists.size > 1) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(
                                    title = "Curated Playlists",
                                    subtitle = "Multi-track collections from independent curators"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(feed.featuredPlaylists, key = { "cur_${it.key}" }) { pl ->
                                        PlaylistSquareCard(
                                            playlist = pl,
                                            onClick = { onOpenPlaylist(pl) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 11. Recently Added Music
                    if (feed.recentlyAddedTracks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Recently Added",
                                subtitle = "Fresh arrivals across streams & previews"
                            )
                        }
                        itemsIndexed(
                            feed.recentlyAddedTracks.take(6),
                            key = { _, t -> "recent_${t.key}" }
                        ) { idx, track ->
                            TrackRowItem(
                                track = track,
                                isCurrent = playbackState.currentTrack?.key == track.key,
                                isPlaying = playbackState.isPlaying,
                                isLiked = likedKeys.contains(track.key),
                                onPlayClick = { onPlayTracks(feed.recentlyAddedTracks, idx) },
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

@Composable
private fun FeaturedHeroCard(
    playlist: CuratedPlaylist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .testTag("featured_hero_card_${playlist.key}")
    ) {
        if (playlist.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = playlist.artworkUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_hero_wave),
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x88070B12),
                            Color(0xE0070B12)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FEATURED MIX • ${playlist.tracks.size} TRACKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            if (playlist.tracks.any { it.isPlayableInApp }) {
                GlassButton(
                    onClick = onPlayClick,
                    variant = GlassVariant.CLEAR,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play ${playlist.title}",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CompactTrackTile(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl.ifBlank { null },
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildMadeForYouMixes(feed: CatalogFeed): List<CuratedPlaylist> {
    val tracks = feed.trendingFullTracks
    if (tracks.isEmpty()) return emptyList()
    val chunks = tracks.chunked(6)
    val titles = listOf("Acoustic Clarity", "Deep Focus Prism", "Nocturnal Frequencies", "Kinetic Pulse")
    return chunks.take(3).mapIndexed { idx, list ->
        CuratedPlaylist(
            key = "made_for_you_$idx",
            source = list.first().source,
            title = titles.getOrElse(idx) { "Wave Mix ${idx + 1}" },
            subtitle = "${list.size} full-length tracks • ${list.first().genre.ifBlank { "Electronic" }}",
            artworkUrl = list.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty(),
            tracks = list
        )
    }
}
