package com.example.ui.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.PlaybackCapability
import com.example.model.Track
import com.example.playback.PlaybackUiState

/**
 * Core Liquid Glass material container.
 *
 * Separates the optical backdrop layer (AGSL refraction + RenderEffect blur) from the foreground
 * content layer so icons, labels, and controls remain razor-sharp at every quality tier.
 */
@Composable
fun GlassMaterial(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.FROSTED,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    customAccent: Color? = null,
    overrideTier: GlassRenderTier? = null,
    overrideBackdrop: GlassBackdropState? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassContext.current
    val effectiveTier = overrideTier ?: glass.tier
    val backdropState = overrideBackdrop ?: glass.backdropState
    val spec = resolveGlassSpec(
        variant = variant,
        isDark = glass.isDark,
        accentColor = customAccent ?: glass.accentColor
    )
    val density = LocalDensity.current
    val cornerPx = with(density) { cornerRadius.toPx() }
    val blurPx = with(density) { spec.blurRadius.toPx() }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }

    val specularBrush = remember(spec, glass.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = spec.borderTopAlpha),
                Color.White.copy(alpha = spec.borderBottomAlpha * 0.6f),
                (if (glass.isDark) Color.White else Color(0xFF94A3B8)).copy(alpha = spec.borderBottomAlpha)
            )
        )
    }

    val surfaceWashBrush = remember(spec, glass.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (glass.isDark) 0.11f else 0.36f),
                spec.tintColor.copy(alpha = spec.tintAlpha),
                spec.tintColor.copy(alpha = (spec.tintAlpha + 0.12f).coerceAtMost(0.88f))
            )
        )
    }

    val layer = backdropState?.backdropLayer
    val backdropOrigin = backdropState?.rootOffset ?: Offset.Zero

    Box(
        modifier = modifier
            .shadow(
                elevation = spec.shadowElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.55f else 0.16f),
                spotColor = Color.Black.copy(alpha = if (glass.isDark) 0.65f else 0.20f)
            )
            .clip(shape)
            .onGloballyPositioned { coords ->
                boxSize = coords.size
                rootPos = coords.positionInRoot()
            }
    ) {
        // 1. Optical Backdrop Layer (Refracted/Blurred background pixels, isolated from foreground text)
        if (effectiveTier != GlassRenderTier.FALLBACK && layer != null && boxSize.width > 0 && boxSize.height > 0) {
            val renderEffect = remember(
                effectiveTier,
                boxSize,
                cornerPx,
                blurPx,
                spec
            ) {
                when {
                    effectiveTier == GlassRenderTier.HIGH &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        LiquidGlassAgsl.createGlassRenderEffect(
                            widthPx = boxSize.width.toFloat(),
                            heightPx = boxSize.height.toFloat(),
                            cornerRadiusPx = cornerPx,
                            blurRadiusPx = blurPx,
                            refractionStrength = spec.refractionStrength,
                            chromaticAberration = spec.chromaticAberration,
                            specularIntensity = spec.specularIntensity,
                            tintRed = spec.tintColor.red,
                            tintGreen = spec.tintColor.green,
                            tintBlue = spec.tintColor.blue,
                            tintAlpha = spec.tintAlpha * 0.65f
                        ).asComposeRenderEffect()
                    }
                    effectiveTier == GlassRenderTier.STANDARD &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        RenderEffect.createBlurEffect(
                            blurPx,
                            blurPx,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                    else -> null
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.renderEffect = renderEffect
                        clip = true
                        this.shape = shape
                    }
                    .drawBehind {
                        val dx = backdropOrigin.x - rootPos.x
                        val dy = backdropOrigin.y - rootPos.y
                        drawContext.canvas.save()
                        drawContext.canvas.translate(dx, dy)
                        drawLayer(layer)
                        drawContext.canvas.restore()
                    }
            )
        }

        // 2. Depth & Specular Tint Wash Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (effectiveTier == GlassRenderTier.FALLBACK) {
                        Brush.verticalGradient(
                            colors = listOf(
                                spec.tintColor.copy(alpha = (spec.tintAlpha + 0.34f).coerceAtMost(0.95f)),
                                spec.tintColor.copy(alpha = (spec.tintAlpha + 0.42f).coerceAtMost(0.97f))
                            )
                        )
                    } else {
                        surfaceWashBrush
                    }
                )
                .border(BorderStroke(1.dp, specularBrush), shape)
        )

        // 3. Foreground Content Layer (Un-blurred, un-distorted, crisp typography & icons)
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.FROSTED,
    cornerRadius: Dp = 20.dp,
    customAccent: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    GlassMaterial(
        modifier = modifier,
        variant = variant,
        cornerRadius = cornerRadius,
        customAccent = customAccent,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.FROSTED,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !glass.reducedMotion) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glassCardScale"
    )

    GlassMaterial(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                } else Modifier
            ),
        variant = variant,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun GlassHeader(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.FROSTED,
    content: @Composable RowScope.() -> Unit
) {
    GlassMaterial(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        variant = variant,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            content = content
        )
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.INTERACTIVE,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val glass = LocalGlassContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !glass.reducedMotion) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glassButtonScale"
    )

    GlassMaterial(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            ),
        variant = variant,
        cornerRadius = 18.dp,
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.INTERACTIVE,
    size: Dp = 48.dp,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    GlassMaterial(
        modifier = modifier
            .size(size)
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        variant = variant,
        cornerRadius = size / 2,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

data class DockDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun GlassDock(
    destinations: List<DockDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassMaterial(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            variant = if (glass.isDark) GlassVariant.DARK else GlassVariant.FROSTED,
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            glass.accentColor.copy(alpha = if (glass.isDark) 0.24f else 0.18f)
                        } else {
                            Color.Transparent
                        },
                        label = "dockPillColor"
                    )
                    val borderAlpha by animateFloatAsState(
                        targetValue = if (selected) 0.45f else 0f,
                        label = "dockPillBorder"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            if (glass.isDark) Color.White else MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "dockIconTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillColor)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(
                                role = Role.Tab,
                                onClick = { onNavigate(dest.route) }
                            )
                            .testTag(dest.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.icon,
                                contentDescription = dest.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassMiniPlayer(
    playbackState: PlaybackUiState,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track: Track = playbackState.currentTrack ?: return
    val glass = LocalGlassContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassMaterial(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clickable(onClick = onOpenNowPlaying)
                .testTag("mini_player_bar"),
            variant = if (glass.isDark) GlassVariant.DARK else GlassVariant.FROSTED,
            cornerRadius = 22.dp,
            customAccent = glass.accentColor,
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.artworkUrl.ifBlank { null },
                        contentDescription = "${track.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (track.capability == PlaybackCapability.PREVIEW_30S) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "30s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Subtle razor-thin optical progress line integrated along the bottom edge
                val progress = playbackState.progressFraction
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.5.dp)
                            .background(glass.accentColor)
                    )
                }
            }
        }
    }
}
