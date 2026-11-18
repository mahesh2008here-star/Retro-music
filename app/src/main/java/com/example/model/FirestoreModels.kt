package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredQuality: String = AudioQualityPreference.HIGH.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LikedTrackDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackKey: String = "",
    val source: String = MusicSource.AUDIUS.name,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val externalUrl: String = "",
    val durationMs: Long = 0L,
    val capability: String = PlaybackCapability.FULL_STREAM.name,
    val genre: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toTrack(): Track = Track(
        key = trackKey.ifBlank { id },
        source = enumValues<MusicSource>().firstOrNull { it.name == source } ?: MusicSource.AUDIUS,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        externalUrl = externalUrl,
        durationMs = durationMs,
        capability = enumValues<PlaybackCapability>().firstOrNull { it.name == capability }
            ?: PlaybackCapability.METADATA_ONLY,
        genre = genre
    )
}

@Keep
@IgnoreExtraProperties
data class PlaylistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val artworkUrl: String = "",
    val trackCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlaylistTrackDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playlistId: String = "",
    val trackKey: String = "",
    val source: String = MusicSource.AUDIUS.name,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val externalUrl: String = "",
    val durationMs: Long = 0L,
    val capability: String = PlaybackCapability.FULL_STREAM.name,
    val genre: String = "",
    val position: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toTrack(): Track = Track(
        key = trackKey.ifBlank { id },
        source = enumValues<MusicSource>().firstOrNull { it.name == source } ?: MusicSource.AUDIUS,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        externalUrl = externalUrl,
        durationMs = durationMs,
        capability = enumValues<PlaybackCapability>().firstOrNull { it.name == capability }
            ?: PlaybackCapability.METADATA_ONLY,
        genre = genre
    )
}

@Keep
@IgnoreExtraProperties
data class SavedAlbumDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val albumKey: String = "",
    val source: String = MusicSource.ITUNES.name,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val externalUrl: String = "",
    val releaseYear: String = "",
    val trackCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toAlbum(): Album = Album(
        key = albumKey.ifBlank { id },
        source = enumValues<MusicSource>().firstOrNull { it.name == source } ?: MusicSource.ITUNES,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        externalUrl = externalUrl,
        releaseYear = releaseYear,
        trackCount = trackCount.toInt()
    )
}

@Keep
@IgnoreExtraProperties
data class FollowedArtistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val artistKey: String = "",
    val source: String = MusicSource.AUDIUS.name,
    val name: String = "",
    val artworkUrl: String = "",
    val genre: String = "",
    val externalUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toArtist(): Artist = Artist(
        key = artistKey.ifBlank { id },
        source = enumValues<MusicSource>().firstOrNull { it.name == source } ?: MusicSource.AUDIUS,
        name = name,
        artworkUrl = artworkUrl,
        genre = genre,
        externalUrl = externalUrl
    )
}

@Keep
@IgnoreExtraProperties
data class ListeningHistoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackKey: String = "",
    val source: String = MusicSource.AUDIUS.name,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val externalUrl: String = "",
    val durationMs: Long = 0L,
    val capability: String = PlaybackCapability.FULL_STREAM.name,
    val genre: String = "",
    @ServerTimestamp val playedAt: Timestamp? = null
) {
    fun toTrack(): Track = Track(
        key = trackKey.ifBlank { id },
        source = enumValues<MusicSource>().firstOrNull { it.name == source } ?: MusicSource.AUDIUS,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        externalUrl = externalUrl,
        durationMs = durationMs,
        capability = enumValues<PlaybackCapability>().firstOrNull { it.name == capability }
            ?: PlaybackCapability.METADATA_ONLY,
        genre = genre
    )
}

fun Track.toFirestoreTrackFields(uid: String): MutableMap<String, Any> = mutableMapOf(
    "userId" to uid,
    "trackKey" to key.take(128).ifBlank { "unknown" },
    "source" to source.name,
    "title" to title.take(160).ifBlank { "Untitled" },
    "artist" to artist.take(120).ifBlank { "Unknown Artist" },
    "album" to album.take(160),
    "artworkUrl" to artworkUrl.take(500),
    "streamUrl" to streamUrl.take(500),
    "externalUrl" to externalUrl.take(500),
    "durationMs" to durationMs.coerceIn(0L, 86_400_000L),
    "capability" to capability.name,
    "genre" to genre.take(60)
)
