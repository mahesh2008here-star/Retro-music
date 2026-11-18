package com.example.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassVariant {
    CLEAR,
    FROSTED,
    TINTED,
    DARK,
    INTERACTIVE
}

enum class GlassRenderTier(val title: String, val summary: String) {
    HIGH(
        "High — AGSL Optical Refraction",
        "API 33+ AGSL RuntimeShader lens bending, RGB chromatic edge dispersion, backdrop blur & Fresnel specular lighting"
    ),
    STANDARD(
        "Standard — Translucent Blur",
        "Optimized RenderEffect backdrop diffusion with multi-stop Fresnel specular edge highlights"
    ),
    FALLBACK(
        "Fallback — Low-Overhead Surface",
        "Crisp tinted translucent material without offscreen blur or shader passes"
    )
}

data class GlassMaterialSpec(
    val tintColor: Color,
    val tintAlpha: Float,
    val blurRadius: Dp,
    val refractionStrength: Float,
    val chromaticAberration: Float,
    val specularIntensity: Float,
    val shadowElevation: Dp,
    val borderTopAlpha: Float,
    val borderBottomAlpha: Float
)

fun resolveGlassSpec(
    variant: GlassVariant,
    isDark: Boolean,
    accentColor: Color
): GlassMaterialSpec = when (variant) {
    GlassVariant.CLEAR -> GlassMaterialSpec(
        tintColor = if (isDark) Color(0xFF141A24) else Color(0xFFFDFEFF),
        tintAlpha = if (isDark) 0.28f else 0.34f,
        blurRadius = 10.dp,
        refractionStrength = 26f,
        chromaticAberration = 2.6f,
        specularIntensity = 0.72f,
        shadowElevation = 10.dp,
        borderTopAlpha = if (isDark) 0.45f else 0.85f,
        borderBottomAlpha = if (isDark) 0.14f else 0.28f
    )
    GlassVariant.FROSTED -> GlassMaterialSpec(
        tintColor = if (isDark) Color(0xFF151C28) else Color(0xFFF9FAF7),
        tintAlpha = if (isDark) 0.56f else 0.64f,
        blurRadius = 20.dp,
        refractionStrength = 18f,
        chromaticAberration = 1.6f,
        specularIntensity = 0.58f,
        shadowElevation = 12.dp,
        borderTopAlpha = if (isDark) 0.38f else 0.78f,
        borderBottomAlpha = if (isDark) 0.12f else 0.22f
    )
    GlassVariant.TINTED -> GlassMaterialSpec(
        tintColor = accentColor,
        tintAlpha = if (isDark) 0.30f else 0.22f,
        blurRadius = 18.dp,
        refractionStrength = 22f,
        chromaticAberration = 2.2f,
        specularIntensity = 0.68f,
        shadowElevation = 12.dp,
        borderTopAlpha = if (isDark) 0.48f else 0.75f,
        borderBottomAlpha = if (isDark) 0.16f else 0.26f
    )
    GlassVariant.DARK -> GlassMaterialSpec(
        tintColor = Color(0xFF0A0E16),
        tintAlpha = if (isDark) 0.68f else 0.54f,
        blurRadius = 22.dp,
        refractionStrength = 20f,
        chromaticAberration = 1.8f,
        specularIntensity = 0.54f,
        shadowElevation = 16.dp,
        borderTopAlpha = 0.34f,
        borderBottomAlpha = 0.10f
    )
    GlassVariant.INTERACTIVE -> GlassMaterialSpec(
        tintColor = if (isDark) Color(0xFF1A2232) else Color(0xFFFFFFFF),
        tintAlpha = if (isDark) 0.48f else 0.58f,
        blurRadius = 16.dp,
        refractionStrength = 24f,
        chromaticAberration = 2.4f,
        specularIntensity = 0.80f,
        shadowElevation = 8.dp,
        borderTopAlpha = if (isDark) 0.52f else 0.88f,
        borderBottomAlpha = if (isDark) 0.16f else 0.30f
    )
}
