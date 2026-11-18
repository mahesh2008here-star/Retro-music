package com.example.model

enum class MusicSource(val label: String) {
    AUDIUS("Audius"),
    ITUNES("iTunes")
}

enum class PlaybackCapability(val badgeLabel: String, val detailLabel: String) {
    FULL_STREAM("Full Track", "Full stream playable inside MasterWave"),
    PREVIEW_30S("30s Preview", "Official 30-second audio preview"),
    METADATA_ONLY("Catalog Info", "Metadata only — no in-app audio stream"),
    EXTERNAL_LINK("External", "Opens in external music service")
}

enum class AudioQualityPreference(val label: String, val description: String) {
    HIGH("High (Lossless / 320 kbps)", "Best available stream clarity when supported by source"),
    STANDARD("Balanced (192 kbps)", "Smooth playback over mobile networks"),
    DATA_SAVER("Data Saver (96 kbps)", "Reduced artwork & bandwidth usage")
}

enum class ThemeModePreference(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class GlassQualityMode(val label: String, val description: String) {
    AUTO("Adaptive (Recommended)", "Automatically picks AGSL optical refraction on Android 13+ or optimized blur"),
    HIGH("High — Optical Refraction", "AGSL lens refraction, chromatic rim & live backdrop sampling"),
    STANDARD("Standard — Frosted Blur", "Optimized translucent blur with Fresnel edge highlights"),
    FALLBACK("Fallback — Low Overhead", "Crisp tinted surfaces without heavy shader passes")
}

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

data class Track(
    val key: String,
    val source: MusicSource,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val externalUrl: String = "",
    val durationMs: Long = 0L,
    val capability: PlaybackCapability = PlaybackCapability.METADATA_ONLY,
    val genre: String = "",
    val playCount: Long = 0L
) {
    val isPlayableInApp: Boolean
        get() = (capability == PlaybackCapability.FULL_STREAM || capability == PlaybackCapability.PREVIEW_30S) &&
            streamUrl.isNotBlank()

    val safeFirestoreId: String
        get() = sanitizeFirestoreId(key)
}

data class Album(
    val key: String,
    val source: MusicSource,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val externalUrl: String = "",
    val releaseYear: String = "",
    val trackCount: Int = 0,
    val genre: String = ""
) {
    val safeFirestoreId: String
        get() = sanitizeFirestoreId(key)
}

data class Artist(
    val key: String,
    val source: MusicSource,
    val name: String,
    val artworkUrl: String = "",
    val genre: String = "",
    val externalUrl: String = "",
    val followerCount: Long = 0L,
    val bio: String = ""
) {
    val safeFirestoreId: String
        get() = sanitizeFirestoreId(key)
}

data class CuratedPlaylist(
    val key: String,
    val source: MusicSource,
    val title: String,
    val subtitle: String,
    val artworkUrl: String = "",
    val externalUrl: String = "",
    val tracks: List<Track> = emptyList(),
    val trackCount: Int = tracks.size
)

data class GenreMood(
    val id: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val accentHex: Long,
    val isMood: Boolean = false
)

data class SearchResults(
    val query: String = "",
    val songs: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<CuratedPlaylist> = emptyList()
)

fun sanitizeFirestoreId(raw: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .take(120)
    return cleaned.ifBlank { "item_${raw.hashCode().toUInt()}" }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
