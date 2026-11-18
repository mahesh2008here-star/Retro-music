package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.RepeatModeState
import com.example.model.formatDurationMs
import com.example.playback.PlaybackUiState
import com.example.ui.artwork.ArtworkPalette
import com.example.ui.components.CapabilityBadge
import com.example.ui.glass.GlassBackdropSource
import com.example.ui.glass.GlassIconButton
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.GlassVariant
import com.example.ui.glass.LocalGlassContext

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    palette: ArtworkPalette,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onMoreClick: () -> Unit,
    onOpenExternal: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val track = playbackState.currentTrack
    val glass = LocalGlassContext.current

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValueMs by remember { mutableFloatStateOf(0f) }

    val artworkScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying || glass.reducedMotion) 1f else 0.93f,
        animationSpec = spring(),
        label = "artworkScale"
    )

    var verticalDragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090F))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccumulated = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragAccumulated += dragAmount
                        if (verticalDragAccumulated > 150f) {
                            onDismiss()
                        }
                    }
                )
            }
    ) {
        // Artwork-derived adaptive background captured in GlassBackdropSource
        GlassBackdropSource(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.dominant.copy(alpha = 0.82f),
                                palette.muted.copy(alpha = 0.72f),
                                Color(0xFF07090F)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.glowPrimary,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    onClick = onDismiss,
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize player",
                    variant = GlassVariant.CLEAR,
                    iconTint = Color.White,
                    modifier = Modifier.testTag("now_playing_minimize")
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    if (track != null) {
                        Text(
                            text = track.source.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                GlassIconButton(
                    onClick = onMoreClick,
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = "Track options",
                    variant = GlassVariant.CLEAR,
                    iconTint = Color.White
                )
            }

            Spacer(Modifier.height(18.dp))

            if (track == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No track selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            } else {
                // Dominant Artwork with horizontal swipe gesture for Next/Previous
                var horizontalAccumulated by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                        }
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF141926))
                        .pointerInput(track.key) {
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalAccumulated = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalAccumulated += dragAmount
                                },
                                onDragEnd = {
                                    if (horizontalAccumulated < -120f) onNext()
                                    else if (horizontalAccumulated > 120f) onPrevious()
                                    horizontalAccumulated = 0f
                                }
                            )
                        }
                ) {
                    AnimatedContent(targetState = track.artworkUrl, label = "npArtwork") { artUrl ->
                        if (artUrl.isNotBlank()) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = "${track.title} album art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_editorial_midnight),
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Title, Artist, Album, Capability Badge & Like
                Column(
                    modifier = Modifier
                        .widthIn(max = 540.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("now_playing_title")
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = listOfNotNull(
                                    track.artist,
                                    track.album.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("now_playing_like_button")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) palette.vibrant else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapabilityBadge(capability = track.capability)
                        if (track.externalUrl.isNotBlank()) {
                            Surface(
                                onClick = { onOpenExternal(track.externalUrl) },
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Open source",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Playback Error Banner
                    playbackState.errorMessage?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = Color(0x44EF4444),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissError) {
                                    Text("Dismiss", color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Playback Scrubber
                    val duration = playbackState.durationMs.coerceAtLeast(1L)
                    val sliderVal = if (isScrubbing) scrubValueMs else playbackState.positionMs.toFloat()
                    Slider(
                        value = sliderVal.coerceIn(0f, duration.toFloat()),
                        onValueChange = {
                            isScrubbing = true
                            scrubValueMs = it
                        },
                        onValueChangeFinished = {
                            onSeekTo(scrubValueMs.toLong())
                            isScrubbing = false
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = palette.vibrant,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val displayedPos = if (isScrubbing) scrubValueMs.toLong() else playbackState.positionMs
                    val remaining = (playbackState.durationMs - displayedPos).coerceAtLeast(0L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(displayedPos),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(
                            text = "-${formatDurationMs(remaining)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Liquid Glass Control Deck
                    GlassSurface(
                        variant = GlassVariant.CLEAR,
                        cornerRadius = 28.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playbackState.shuffleEnabled) palette.vibrant else Color.White.copy(alpha = 0.65f)
                                )
                            }

                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("now_playing_previous")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous track",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier
                                    .size(66.dp)
                                    .testTag("now_playing_play_pause")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (playbackState.isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(26.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color(0xFF0B0F17)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = Color(0xFF0B0F17),
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("now_playing_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next track",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(
                                onClick = onCycleRepeat,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.repeatMode == RepeatModeState.ONE) {
                                        Icons.Rounded.RepeatOne
                                    } else {
                                        Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Repeat mode",
                                    tint = if (playbackState.repeatMode != RepeatModeState.OFF) palette.vibrant else Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Volume & Queue bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = playbackState.volume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.85f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        GlassIconButton(
                            onClick = onOpenQueue,
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Open queue",
                            variant = GlassVariant.CLEAR,
                            iconTint = Color.White,
                            modifier = Modifier.testTag("now_playing_queue_button")
                        )
                    }
                }
            }
        }
    }
}
