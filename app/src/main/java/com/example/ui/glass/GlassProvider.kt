package com.example.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.example.model.GlassQualityMode
import com.example.model.ThemeModePreference
import com.example.ui.theme.WaveCyan

@Stable
class GlassBackdropState(
    val backdropLayer: GraphicsLayer?
) {
    var rootOffset by mutableStateOf(Offset.Zero)
}

@Stable
data class GlassContextState(
    val tier: GlassRenderTier = GlassRenderTier.STANDARD,
    val isDark: Boolean = true,
    val accentColor: Color = WaveCyan,
    val reducedMotion: Boolean = false,
    val backdropState: GlassBackdropState? = null
)

val LocalGlassContext = compositionLocalOf { GlassContextState() }

@Composable
fun GlassProvider(
    themeMode: ThemeModePreference,
    glassQualityMode: GlassQualityMode,
    accentColor: Color,
    reducedMotion: Boolean,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.DARK -> true
        ThemeModePreference.LIGHT -> false
    }
    val resolvedTier = remember(glassQualityMode, reducedMotion) {
        LiquidGlassCapabilities.resolveTier(glassQualityMode, reducedMotion)
    }
    val backdropLayer = if (resolvedTier != GlassRenderTier.FALLBACK) {
        rememberGraphicsLayer()
    } else null

    val backdropState = remember(backdropLayer) {
        GlassBackdropState(backdropLayer)
    }

    val contextState = remember(resolvedTier, isDark, accentColor, reducedMotion, backdropState) {
        GlassContextState(
            tier = resolvedTier,
            isDark = isDark,
            accentColor = accentColor,
            reducedMotion = reducedMotion,
            backdropState = backdropState
        )
    }

    CompositionLocalProvider(LocalGlassContext provides contextState) {
        content()
    }
}

/**
 * Records its child content into the shared GraphicsLayer backdrop so floating Liquid Glass
 * overlays (GlassDock, GlassMiniPlayer, GlassHeader) can sample and optically refract the
 * live pixels beneath them.
 */
@Composable
fun GlassBackdropSource(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassContext.current
    val layer = glass.backdropState?.backdropLayer
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                glass.backdropState?.rootOffset = coords.positionInRoot()
            }
            .then(
                if (layer != null) {
                    Modifier.drawWithContent {
                        layer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(layer)
                    }
                } else Modifier
            ),
        content = content
    )
}
