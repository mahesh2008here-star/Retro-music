package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UserPreferencesState
import com.example.model.AudioQualityPreference
import com.example.model.GlassQualityMode
import com.example.model.ThemeModePreference
import com.example.ui.glass.GlassBackdropState
import com.example.ui.glass.GlassHeader
import com.example.ui.glass.GlassMaterial
import com.example.ui.glass.GlassRenderTier
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.GlassVariant
import com.example.ui.glass.LiquidGlassCapabilities
import com.example.ui.glass.LocalGlassContext
import com.example.ui.theme.WaveAmber
import com.example.ui.theme.WaveCyan
import kotlin.math.roundToInt

private enum class OpticsTestScene(val label: String) {
    ARTWORK("Colorful Art"),
    DARK("Dark"),
    LIGHT("Light"),
    GRADIENT("Gradient"),
    MOVING("Moving Content")
}

@Composable
fun SettingsScreen(
    userName: String,
    userEmail: String?,
    userPhotoUrl: String?,
    preferences: UserPreferencesState,
    onBack: () -> Unit,
    onSelectTheme: (ThemeModePreference) -> Unit,
    onSelectGlassQuality: (GlassQualityMode) -> Unit,
    onSelectAudioQuality: (AudioQualityPreference) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleDataSaver: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val glass = LocalGlassContext.current

    Column(modifier = modifier.fillMaxSize()) {
        GlassHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = glass.tier.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Account Card
            item {
                GlassSurface(
                    variant = GlassVariant.FROSTED,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleMedium)
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Google Cloud Library Sync Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sign out")
                        }
                    }
                }
            }

            // 2. Theme Appearance
            item {
                GlassSurface(
                    variant = GlassVariant.FROSTED,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModePreference.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = { onSelectTheme(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Liquid Glass Material Tier & Live Optics Verification Lab
            item {
                GlassSurface(
                    variant = GlassVariant.FROSTED,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Liquid Glass Optics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Active tier: ${glass.tier.title}. ${glass.tier.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        GlassQualityMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectGlassQuality(mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.glassQuality == mode,
                                    onClick = { onSelectGlassQuality(mode) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        LiquidGlassOpticsLab(activeTier = glass.tier)
                    }
                }
            }

            // 4. Playback & Data Preferences
            item {
                GlassSurface(
                    variant = GlassVariant.FROSTED,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Playback & Performance", style = MaterialTheme.typography.titleMedium)

                        AudioQualityPreference.entries.forEach { quality ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectAudioQuality(quality) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.audioQuality == quality,
                                    onClick = { onSelectAudioQuality(quality) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quality.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        quality.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reduced Motion", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Minimizes spring scale and shader motion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.reducedMotion,
                                onCheckedChange = onToggleReducedMotion,
                                modifier = Modifier.testTag("reduced_motion_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data Saver", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Requests lower-resolution artwork on mobile data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.dataSaver,
                                onCheckedChange = onToggleDataSaver
                            )
                        }

                        Button(
                            onClick = onClearCache,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Catalog, Palette & Search Cache")
                        }
                    }
                }
            }

            // 5. About & Privacy
            item {
                GlassSurface(
                    variant = GlassVariant.FROSTED,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About MasterWave Music", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Catalog Sources: Audius Public Discovery API (full independent/CC streams) & iTunes Search API (official 30-second previews & album metadata).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Hardware Support: AGSL RuntimeShader optical refraction ${if (LiquidGlassCapabilities.supportsAgslRuntimeShader()) "supported" else "unavailable on this API"}; RenderEffect blur ${if (LiquidGlassCapabilities.supportsRenderEffectBlur()) "supported" else "unavailable on this API"}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Privacy: Your playlists, liked tracks, saved albums, followed artists, and history are isolated to your signed-in Google UID in Firebase Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive test harness verifying Liquid Glass optical refraction, blur, and specular
 * edge lighting across Colorful Artwork, Dark, Light, Gradient, and Moving Content.
 */
@Composable
private fun LiquidGlassOpticsLab(activeTier: GlassRenderTier) {
    var selectedScene by remember { mutableStateOf(OpticsTestScene.ARTWORK) }
    var selectedVariant by remember { mutableStateOf(GlassVariant.CLEAR) }

    val testLayer = rememberGraphicsLayer()
    val testBackdrop = remember(testLayer) { GlassBackdropState(testLayer) }

    val infiniteTransition = rememberInfiniteTransition(label = "opticsLabMove")
    val drift by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opticsDrift"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Live Optics Verification Lab", style = MaterialTheme.typography.titleSmall)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(OpticsTestScene.entries) { scene ->
                FilterChip(
                    selected = selectedScene == scene,
                    onClick = { selectedScene = scene },
                    label = { Text(scene.label) }
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(GlassVariant.entries) { variant ->
                FilterChip(
                    selected = selectedVariant == variant,
                    onClick = { selectedVariant = variant },
                    label = { Text(variant.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            OpticsBackdropRecorder(
                backdropState = testBackdrop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedScene) {
                    OpticsTestScene.ARTWORK -> {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_wave),
                            contentDescription = "Artwork test scene",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OpticsTestScene.DARK -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A0D14))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(WaveCyan.copy(alpha = 0.7f)))
                                Box(Modifier.size(44.dp).clip(CircleShape).background(WaveAmber.copy(alpha = 0.7f)))
                            }
                        }
                    }
                    OpticsTestScene.LIGHT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF3F4EE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEA580C)))
                            }
                        }
                    }
                    OpticsTestScene.GRADIENT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0EA5E9), Color(0xFF4F46E5), Color(0xFFF59E0B))
                                    )
                                )
                        )
                    }
                    OpticsTestScene.MOVING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.offset { IntOffset(drift.roundToInt(), 0) },
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(5) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 24.dp, height = 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (idx % 2 == 0) WaveCyan else WaveAmber)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Liquid Glass Lens Pill over the selected test backdrop
            GlassMaterial(
                variant = selectedVariant,
                cornerRadius = 22.dp,
                overrideTier = activeTier,
                overrideBackdrop = testBackdrop,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                modifier = Modifier.width(230.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Liquid Glass • ${selectedVariant.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selectedScene == OpticsTestScene.LIGHT) Color(0xFF0F172A) else Color.White
                    )
                    Text(
                        text = "${activeTier.name} over ${selectedScene.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedScene == OpticsTestScene.LIGHT) Color(0xFF334155) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OpticsBackdropRecorder(
    backdropState: GlassBackdropState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val layer = backdropState.backdropLayer
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                backdropState.rootOffset = coords.positionInRoot()
            }
            .then(
                if (layer != null) {
                    Modifier.drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                } else Modifier
            )
    ) {
        content()
    }
}
