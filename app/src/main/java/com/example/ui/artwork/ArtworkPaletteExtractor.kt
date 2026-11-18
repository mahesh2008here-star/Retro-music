package com.example.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ui.theme.WaveCyan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkPalette(
    val dominant: Color = Color(0xFF101928),
    val vibrant: Color = WaveCyan,
    val muted: Color = Color(0xFF1E293B),
    val glowPrimary: Color = WaveCyan.copy(alpha = 0.42f),
    val glowSecondary: Color = Color(0xFF0F172A)
)

class ArtworkPaletteExtractor(context: Context) {

    private val appContext = context.applicationContext
    private val imageLoader = ImageLoader(appContext)
    private val cache = LruCache<String, ArtworkPalette>(64)

    suspend fun extract(artworkUrl: String): ArtworkPalette {
        if (artworkUrl.isBlank()) return ArtworkPalette()
        cache.get(artworkUrl)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(appContext)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .size(160, 160)
                    .build()

                val result = imageLoader.execute(request)
                val bitmap: Bitmap? = (result as? SuccessResult)
                    ?.drawable
                    ?.let { it as? BitmapDrawable }
                    ?.bitmap

                if (bitmap == null) return@withContext ArtworkPalette()

                val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                val dominantInt = palette.getDominantColor(0xFF101928.toInt())
                val vibrantInt = palette.getVibrantColor(
                    palette.getLightVibrantColor(WaveCyan.toArgb())
                )
                val mutedInt = palette.getDarkMutedColor(
                    palette.getMutedColor(0xFF1E293B.toInt())
                )

                val accessibleAccent = ensureContrastAccent(Color(vibrantInt))
                val extracted = ArtworkPalette(
                    dominant = Color(dominantInt),
                    vibrant = accessibleAccent,
                    muted = Color(mutedInt),
                    glowPrimary = accessibleAccent.copy(alpha = 0.45f),
                    glowSecondary = Color(dominantInt).copy(alpha = 0.55f)
                )
                cache.put(artworkUrl, extracted)
                extracted
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ArtworkPalette()
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
    }

    private fun ensureContrastAccent(raw: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(raw.toArgb(), hsl)
        // Clamp saturation and lightness so glass accents never blow out or go muddy
        hsl[1] = hsl[1].coerceIn(0.32f, 0.82f)
        hsl[2] = hsl[2].coerceIn(0.46f, 0.68f)
        return Color(ColorUtils.HSLToColor(hsl))
    }
}
