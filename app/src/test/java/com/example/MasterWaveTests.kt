package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.GlassQualityMode
import com.example.model.MusicSource
import com.example.model.PlaybackCapability
import com.example.model.ThemeModePreference
import com.example.model.Track
import com.example.model.formatDurationMs
import com.example.model.sanitizeFirestoreId
import com.example.playback.PlaybackUiState
import com.example.ui.components.TrackRowItem
import com.example.ui.glass.DockDestination
import com.example.ui.glass.GlassDock
import com.example.ui.glass.GlassMiniPlayer
import com.example.ui.glass.GlassProvider
import com.example.ui.glass.GlassRenderTier
import com.example.ui.glass.GlassVariant
import com.example.ui.glass.LiquidGlassCapabilities
import com.example.ui.glass.resolveGlassSpec
import com.example.ui.theme.MasterWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class MasterWaveUnitTest {

    @Test
    fun `track isPlayableInApp honors capability and streamUrl`() {
        val full = Track(
            key = "audius_1",
            source = MusicSource.AUDIUS,
            title = "Midnight Signal",
            artist = "Nova",
            streamUrl = "https://example.com/stream.mp3",
            capability = PlaybackCapability.FULL_STREAM
        )
        val preview = full.copy(
            key = "itunes_1",
            source = MusicSource.ITUNES,
            capability = PlaybackCapability.PREVIEW_30S
        )
        val blankStream = full.copy(streamUrl = "   ")
        val metadataOnly = full.copy(capability = PlaybackCapability.METADATA_ONLY)
        val externalOnly = full.copy(capability = PlaybackCapability.EXTERNAL_LINK)

        assertTrue(full.isPlayableInApp)
        assertTrue(preview.isPlayableInApp)
        assertFalse(blankStream.isPlayableInApp)
        assertFalse(metadataOnly.isPlayableInApp)
        assertFalse(externalOnly.isPlayableInApp)
    }

    @Test
    fun `sanitizeFirestoreId guarantees rule-compliant document id`() {
        val dirty = "audius/track:hello world?foo=bar&x=1"
        val clean = sanitizeFirestoreId(dirty)
        assertTrue(clean.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
        assertTrue(clean.length in 1..120)

        val empty = sanitizeFirestoreId("")
        assertTrue(empty.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
    }

    @Test
    fun `formatDurationMs formats seconds cleanly`() {
        assertEquals("0:00", formatDurationMs(-100L))
        assertEquals("0:00", formatDurationMs(0L))
        assertEquals("0:30", formatDurationMs(30_000L))
        assertEquals("3:05", formatDurationMs(185_000L))
    }

    @Test
    fun `resolveGlassSpec returns valid material parameters for all variants`() {
        for (variant in GlassVariant.entries) {
            for (dark in listOf(true, false)) {
                val spec = resolveGlassSpec(variant, dark, Color(0xFF38BDF8))
                assertTrue(spec.tintAlpha in 0f..1f)
                assertTrue(spec.specularIntensity in 0f..1f)
                assertTrue(spec.borderTopAlpha >= spec.borderBottomAlpha)
            }
        }
    }

    @Test
    fun `reduced motion forces fallback tier for zero-jank accessibility`() {
        assertEquals(
            GlassRenderTier.FALLBACK,
            LiquidGlassCapabilities.resolveTier(GlassQualityMode.HIGH, reducedMotion = true)
        )
        assertEquals(
            GlassRenderTier.FALLBACK,
            LiquidGlassCapabilities.resolveTier(GlassQualityMode.FALLBACK, reducedMotion = false)
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MasterWaveRobolectricUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `metadata only track hides play button and shows catalog badge`() {
        val metadataTrack = Track(
            key = "itunes_meta_1",
            source = MusicSource.ITUNES,
            title = "Instrumental Sketch",
            artist = "Studio Archive",
            capability = PlaybackCapability.METADATA_ONLY,
            durationMs = 180_000L
        )
        composeRule.setContent {
            MasterWaveTheme(themeMode = ThemeModePreference.DARK) {
                TrackRowItem(
                    track = metadataTrack,
                    isCurrent = false,
                    isPlaying = false,
                    isLiked = false,
                    onPlayClick = {},
                    onOpenExternal = {},
                    onToggleLike = {},
                    onMoreClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Catalog Info").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Metadata only").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play Instrumental Sketch").assertDoesNotExist()
    }

    @Test
    fun `playable track shows play button and full track badge`() {
        val playableTrack = Track(
            key = "audius_play_1",
            source = MusicSource.AUDIUS,
            title = "Pulse Horizon",
            artist = "Kairo",
            streamUrl = "https://example.com/pulse.mp3",
            capability = PlaybackCapability.FULL_STREAM,
            durationMs = 210_000L
        )
        var played = false
        composeRule.setContent {
            MasterWaveTheme(themeMode = ThemeModePreference.DARK) {
                TrackRowItem(
                    track = playableTrack,
                    isCurrent = false,
                    isPlaying = false,
                    isLiked = false,
                    onPlayClick = { played = true },
                    onOpenExternal = {},
                    onToggleLike = {},
                    onMoreClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Full Track").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play Pulse Horizon")
            .assertIsDisplayed()
            .performClick()
        assertTrue(played)
    }

    @Test
    fun `glass dock and mini player handle navigation and preview badge`() {
        var currentRoute = "home"
        var toggledPlay = false
        var skippedNext = false
        val previewTrack = Track(
            key = "itunes_prev_1",
            source = MusicSource.ITUNES,
            title = "Glass Drift",
            artist = "Aurelia",
            streamUrl = "https://example.com/preview.m4a",
            capability = PlaybackCapability.PREVIEW_30S,
            durationMs = 30_000L
        )
        val destinations = listOf(
            DockDestination("home", "Home", Icons.Outlined.Home, Icons.Rounded.Home, "nav_home"),
            DockDestination("explore", "Explore", Icons.Outlined.Explore, Icons.Rounded.Explore, "nav_explore"),
            DockDestination("search", "Search", Icons.Outlined.Search, Icons.Rounded.Search, "nav_search"),
            DockDestination("library", "Library", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic, "nav_library")
        )

        composeRule.setContent {
            MasterWaveTheme(themeMode = ThemeModePreference.LIGHT) {
                GlassProvider(
                    themeMode = ThemeModePreference.LIGHT,
                    glassQualityMode = GlassQualityMode.FALLBACK,
                    accentColor = Color(0xFF0284C7),
                    reducedMotion = true
                ) {
                    Column {
                        GlassMiniPlayer(
                            playbackState = PlaybackUiState(
                                currentTrack = previewTrack,
                                isPlaying = false,
                                positionMs = 15_000L,
                                durationMs = 30_000L
                            ),
                            onOpenNowPlaying = {},
                            onTogglePlayPause = { toggledPlay = true },
                            onNext = { skippedNext = true }
                        )
                        GlassDock(
                            destinations = destinations,
                            currentRoute = currentRoute,
                            onNavigate = { currentRoute = it }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
        composeRule.onNodeWithText("30s").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player_play_pause").performClick()
        composeRule.onNodeWithTag("mini_player_next").performClick()
        assertTrue(toggledPlay)
        assertTrue(skippedNext)

        composeRule.onNodeWithTag("nav_explore").performClick()
        assertEquals("explore", currentRoute)
        composeRule.onNodeWithTag("nav_library").performClick()
        assertEquals("library", currentRoute)
    }

    @Test
    fun `glass mini player is hidden when no active track`() {
        composeRule.setContent {
            MasterWaveTheme(themeMode = ThemeModePreference.DARK) {
                GlassProvider(
                    themeMode = ThemeModePreference.DARK,
                    glassQualityMode = GlassQualityMode.FALLBACK,
                    accentColor = Color(0xFF38BDF8),
                    reducedMotion = true
                ) {
                    GlassMiniPlayer(
                        playbackState = PlaybackUiState(currentTrack = null),
                        onOpenNowPlaying = {},
                        onTogglePlayPause = {},
                        onNext = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag("mini_player_bar").assertDoesNotExist()
    }
}
