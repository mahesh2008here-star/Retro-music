package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.ThemeModePreference

val MasterWaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MasterWaveTheme(
    themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.DARK -> true
        ThemeModePreference.LIGHT -> false
    }

    val primaryDark = accentColor ?: WaveCyan
    val primaryLight = accentColor ?: WaveCyanDeep

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryDark,
            onPrimary = DarkBackground,
            primaryContainer = primaryDark.copy(alpha = 0.22f),
            onPrimaryContainer = DarkOnBackground,
            secondary = WaveSilver,
            onSecondary = DarkBackground,
            secondaryContainer = DarkSurfaceElevated,
            onSecondaryContainer = DarkOnSurface,
            tertiary = WaveAmber,
            onTertiary = DarkBackground,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceMuted,
            outline = DarkOutline,
            outlineVariant = DarkOutline.copy(alpha = 0.5f),
            error = WaveCrimson,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryLight,
            onPrimary = Color.White,
            primaryContainer = primaryLight.copy(alpha = 0.14f),
            onPrimaryContainer = LightOnBackground,
            secondary = LightOnSurface,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightOnSurface,
            tertiary = WaveAmber,
            onTertiary = Color.White,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceMuted,
            outline = LightOutline,
            outlineVariant = LightOutline.copy(alpha = 0.6f),
            error = WaveCrimson,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MasterWaveTypography,
        shapes = MasterWaveShapes,
        content = content
    )
}
