package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WaveCyan

/**
 * Custom MasterWave acoustic crest emblem — three interlocking fluid wave arcs
 * inside a specular glass vessel (never a generic music-note icon).
 */
@Composable
fun MasterWaveBrandMark(
    size: Dp = 36.dp,
    accent: Color = WaveCyan,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF162032),
                        Color(0xFF0B0F17)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            )
            .padding(size * 0.16f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height

            val primaryWave = Path().apply {
                moveTo(w * 0.06f, h * 0.66f)
                cubicTo(w * 0.28f, h * 0.10f, w * 0.50f, h * 0.12f, w * 0.64f, h * 0.52f)
                cubicTo(w * 0.74f, h * 0.80f, w * 0.86f, h * 0.72f, w * 0.94f, h * 0.42f)
            }
            drawPath(
                path = primaryWave,
                brush = Brush.linearGradient(
                    colors = listOf(accent, Color.White),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                ),
                style = Stroke(width = w * 0.13f, cap = StrokeCap.Round)
            )

            val secondaryWave = Path().apply {
                moveTo(w * 0.14f, h * 0.82f)
                cubicTo(w * 0.36f, h * 0.44f, w * 0.54f, h * 0.48f, w * 0.68f, h * 0.78f)
                cubicTo(w * 0.78f, h * 0.96f, w * 0.88f, h * 0.88f, w * 0.94f, h * 0.68f)
            }
            drawPath(
                path = secondaryWave,
                color = Color.White.copy(alpha = 0.72f),
                style = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MasterWaveWordmark(
    accent: Color = WaveCyan,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MasterWaveBrandMark(size = 34.dp, accent = accent)
        Text(
            text = "MasterWave",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
