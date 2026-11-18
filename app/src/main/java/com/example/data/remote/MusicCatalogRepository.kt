package com.example.data.remote

import android.util.Log
import com.example.model.Album
import com.example.model.Artist
import com.example.model.CuratedPlaylist
import com.example.model.GenreMood
import com.example.model.MusicSource
import com.example.model.PlaybackCapability
import com.example.model.SearchResults
import com.example.model.Track
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class CatalogFeed(
    val featuredPlaylists: List<CuratedPlaylist> = emptyList(),
    val trendingFullTracks: List<Track> = emptyList(),
    val newReleasesTracks: List<Track> = emptyList(),
    val recommendedAlbums: List<Album> = emptyList(),
    val popularArtists: List<Artist> = emptyList(),
    val recentlyAddedTracks: List<Track> = emptyList(),
    val providerWarnings: List<String> = emptyList()
)

class MusicCatalogRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val itunesApi: ItunesApiService = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ItunesApiService::class.java)

    private val audiusBootstrapApi: AudiusBootstrapApi = Retrofit.Builder()
        .baseUrl("https://api.audius.co/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AudiusBootstrapApi::class.java)

    @Volatile
    private var cachedAudiusHost: String? = null

    @Volatile
    private var cachedFeed: CatalogFeed? = null
    private var cachedFeedTimestampMs: Long = 0L

    private val fallbackAudiusHosts = listOf(
        "https://discoveryprovider.audius.co",
        "https://discoveryprovider2.audius.co",
        "https://discoveryprovider3.audius.co"
    )

    val genresAndMoods: List<GenreMood> = listOf(
        GenreMood("electronic", "Electronic", "Synth, ambient & deep house", "Electronic", 0xFF0EA5E9),
        GenreMood("lofi", "Lo-Fi & Chill", "Warm beats for deep focus", "lofi chillhop", 0xFF14B8A6, isMood = true),
        GenreMood("hiphop", "Hip-Hop", "Rhythm, bass & lyricism", "Hip-Hop/Rap", 0xFFF59E0B),
        GenreMood("indie", "Indie & Alt", "Dream pop & acoustic textures", "Alternative", 0xFF6366F1),
        GenreMood("jazz", "Jazz & Soul", "Brass, Rhodes & midnight grooves", "Jazz", 0xFFD97706),
        GenreMood("classical", "Acoustic & Neo-Classical", "Piano, strings & spatial halls", "Classical", 0xFF64748B),
        GenreMood("nightdrive", "Night Drive", "Atmospheric nocturnal synthwave", "synthwave midnight", 0xFF3B82F6, isMood = true),
        GenreMood("workout", "Kinetic Energy", "High-tempo momentum cuts", "energetic electronic workout", 0xFFEF4444, isMood = true)
    )

    val quickSuggestions: List<String> = listOf(
        "Electronic", "Ambient", "Fred again", "Khruangbin", "Synthwave",
        "Jazz Piano", "Daft Punk", "Lo-Fi Beats", "Nils Frahm", "Indie Pop"
    )

    private suspend fun resolveAudiusHost(): String {
        cachedAudiusHost?.let { return it }
        return try {
            val nodes = audiusBootstrapApi.getDiscoveryNodes().data
            val picked = nodes.firstOrNull { it.startsWith("https://") }
                ?.trimEnd('/')
                ?: fallbackAudiusHosts.first()
            cachedAudiusHost = picked
            picked
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Audius bootstrap failed, using fallback host", e)
            fallbackAudiusHosts.first()
        }
    }

    private fun createAudiusService(host: String): AudiusApiService =
        Retrofit.Builder()
            .baseUrl("${host.trimEnd('/')}/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AudiusApiService::class.java)

    suspend fun loadHomeCatalog(
        forceRefresh: Boolean = false,
        dataSaver: Boolean = false
    ): CatalogFeed = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = cachedFeed
        if (!forceRefresh && existing != null && (now - cachedFeedTimestampMs) < FEED_CACHE_TTL_MS) {
            return@withContext existing
        }

        val warnings = mutableListOf<String>()
        coroutineScope {
            val audiusTrendingDeferred = async {
                fetchAudiusWithFailover(dataSaver) { api, host ->
                    api.getTrendingTracks(limit = 22).data.mapNotNull { it.toDomainTrack(host, dataSaver) }
                }
            }
            val audiusPlaylistsDeferred = async {
                fetchAudiusWithFailover(dataSaver) { api, host ->
                    api.getTrendingPlaylists(limit = 10).data.map { it.toDomainPlaylist(host, dataSaver) }
                }
            }
            val itunesAlbumsDeferred = async {
                runCatching {
                    itunesApi.search(term = "electronic ambient jazz", entity = "album", limit = 16)
                        .results.mapNotNull { it.toDomainAlbum(dataSaver) }
                        .distinctBy { it.title.lowercase() }
                }
            }
            val itunesArtistsDeferred = async {
                runCatching {
                    itunesApi.search(term = "indie electronic soul", entity = "musicArtist", limit = 14)
                        .results.mapNotNull { it.toDomainArtist(dataSaver) }
                }
            }
            val itunesNewSongsDeferred = async {
                runCatching {
                    itunesApi.search(term = "2025 electronic indie soul", entity = "song", limit = 18)
                        .results.mapNotNull { it.toDomainTrack(dataSaver) }
                        .distinctBy { it.key }
                }
            }

            val trendingTracks = audiusTrendingDeferred.await().getOrElse {
                warnings += "Audius stream catalog temporarily unreachable"
                emptyList()
            }
            val playlists = audiusPlaylistsDeferred.await().getOrElse { emptyList() }
            val albums = itunesAlbumsDeferred.await().getOrElse {
                warnings += "iTunes album catalog temporarily unreachable"
                emptyList()
            }
            val artistsRaw = itunesArtistsDeferred.await().getOrElse { emptyList() }
            val newReleases = itunesNewSongsDeferred.await().getOrElse { emptyList() }

            // Enrich artists with artwork from tracks/albums when iTunes artist entity lacks artwork
            val artByArtist = (trendingTracks + newReleases)
                .filter { it.artworkUrl.isNotBlank() }
                .associate { it.artist.lowercase() to it.artworkUrl }
            val artists = if (artistsRaw.isNotEmpty()) {
                artistsRaw.map { artist ->
                    val fallbackArt = artByArtist[artist.name.lowercase()]
                        ?: trendingTracks.firstOrNull { it.artist.equals(artist.name, ignoreCase = true) }?.artworkUrl
                        ?: ""
                    if (artist.artworkUrl.isBlank()) artist.copy(artworkUrl = fallbackArt) else artist
                }
            } else {
                trendingTracks.distinctBy { it.artist.lowercase() }.take(10).map { t ->
                    Artist(
                        key = "audius_artist_${t.artist.hashCode().toUInt()}",
                        source = MusicSource.AUDIUS,
                        name = t.artist,
                        artworkUrl = t.artworkUrl,
                        genre = t.genre
                    )
                }
            }

            val feed = CatalogFeed(
                featuredPlaylists = playlists.filter { it.tracks.isNotEmpty() }.ifEmpty {
                    if (trendingTracks.isNotEmpty()) {
                        listOf(
                            CuratedPlaylist(
                                key = "audius_curated_wave",
                                source = MusicSource.AUDIUS,
                                title = "MasterWave Signal",
                                subtitle = "Full-length independent electronic & ambient streams",
                                artworkUrl = trendingTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty(),
                                tracks = trendingTracks.take(10)
                            )
                        )
                    } else emptyList()
                },
                trendingFullTracks = trendingTracks,
                newReleasesTracks = newReleases,
                recommendedAlbums = albums,
                popularArtists = artists,
                recentlyAddedTracks = (trendingTracks.drop(8).take(8) + newReleases.take(8)).distinctBy { it.key },
                providerWarnings = warnings
            )

            cachedFeed = feed
            cachedFeedTimestampMs = System.currentTimeMillis()
            feed
        }
    }

    suspend fun searchAll(query: String, dataSaver: Boolean = false): Result<SearchResults> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext Result.success(SearchResults())

            try {
                coroutineScope {
                    val audiusTracksDef = async {
                        fetchAudiusWithFailover(dataSaver) { api, host ->
                            api.searchTracks(query = trimmed, limit = 20).data.mapNotNull {
                                it.toDomainTrack(host, dataSaver)
                            }
                        }.getOrDefault(emptyList())
                    }
                    val audiusUsersDef = async {
                        fetchAudiusWithFailover(dataSaver) { api, _ ->
                            api.searchUsers(query = trimmed, limit = 10).data.map {
                                Artist(
                                    key = "audius_user_${it.id}",
                                    source = MusicSource.AUDIUS,
                                    name = it.name.ifBlank { it.handle },
                                    artworkUrl = it.profilePicture?.best(dataSaver).orEmpty(),
                                    genre = "Independent Artist",
                                    externalUrl = if (it.handle.isNotBlank()) "https://audius.co/${it.handle}" else "",
                                    followerCount = it.followerCount,
                                    bio = it.bio.orEmpty()
                                )
                            }
                        }.getOrDefault(emptyList())
                    }
                    val itunesSongsDef = async {
                        runCatching {
                            itunesApi.search(term = trimmed, entity = "song", limit = 20)
                                .results.mapNotNull { it.toDomainTrack(dataSaver) }
                        }.getOrDefault(emptyList())
                    }
                    val itunesAlbumsDef = async {
                        runCatching {
                            itunesApi.search(term = trimmed, entity = "album", limit = 14)
                                .results.mapNotNull { it.toDomainAlbum(dataSaver) }
                        }.getOrDefault(emptyList())
                    }
                    val itunesArtistsDef = async {
                        runCatching {
                            itunesApi.search(term = trimmed, entity = "musicArtist", limit = 10)
                                .results.mapNotNull { it.toDomainArtist(dataSaver) }
                        }.getOrDefault(emptyList())
                    }

                    val audiusSongs = audiusTracksDef.await()
                    val itunesSongs = itunesSongsDef.await()
                    val combinedSongs = interleave(audiusSongs, itunesSongs)
                    val albums = itunesAlbumsDef.await()
                    val artists = (audiusUsersDef.await() + itunesArtistsDef.await())
                        .distinctBy { it.name.lowercase() }

                    val playlists = if (audiusSongs.isNotEmpty()) {
                        listOf(
                            CuratedPlaylist(
                                key = "search_mix_${trimmed.hashCode().toUInt()}",
                                source = MusicSource.AUDIUS,
                                title = "$trimmed — Stream Mix",
                                subtitle = "${audiusSongs.size} full-length tracks ready to play",
                                artworkUrl = audiusSongs.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty(),
                                tracks = audiusSongs.take(12)
                            )
                        )
                    } else emptyList()

                    Result.success(
                        SearchResults(
                            query = trimmed,
                            songs = combinedSongs,
                            albums = albums,
                            artists = artists,
                            playlists = playlists
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for '$trimmed'", e)
                Result.failure(e)
            }
        }

    suspend fun fetchAlbumTracks(album: Album, dataSaver: Boolean = false): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                val idLong = album.key.removePrefix("itunes_album_").toLongOrNull()
                    ?: return@withContext Result.success(emptyList())
                val response = itunesApi.lookupAlbumTracks(collectionId = idLong)
                val tracks = response.results
                    .filter { it.wrapperType == "track" }
                    .mapNotNull { it.toDomainTrack(dataSaver) }
                Result.success(tracks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchGenreTracks(genre: GenreMood, dataSaver: Boolean = false): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    val audiusDef = async {
                        fetchAudiusWithFailover(dataSaver) { api, host ->
                            if (!genre.isMood) {
                                api.getTrendingTracks(genre = genre.query, limit = 16).data
                                    .mapNotNull { it.toDomainTrack(host, dataSaver) }
                            } else {
                                api.searchTracks(query = genre.query, limit = 16).data
                                    .mapNotNull { it.toDomainTrack(host, dataSaver) }
                            }
                        }.getOrDefault(emptyList())
                    }
                    val itunesDef = async {
                        runCatching {
                            itunesApi.search(term = genre.query, entity = "song", limit = 12)
                                .results.mapNotNull { it.toDomainTrack(dataSaver) }
                        }.getOrDefault(emptyList())
                    }
                    Result.success(interleave(audiusDef.await(), itunesDef.await()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun clearMemoryCache() {
        cachedFeed = null
        cachedFeedTimestampMs = 0L
    }

    private suspend fun <T> fetchAudiusWithFailover(
        dataSaver: Boolean,
        block: suspend (AudiusApiService, String) -> T
    ): Result<T> {
        val primary = resolveAudiusHost()
        val candidates = (listOf(primary) + fallbackAudiusHosts).distinct()
        var lastError: Throwable? = null
        for (host in candidates) {
            try {
                val service = createAudiusService(host)
                val result = block(service, host)
                cachedAudiusHost = host
                return Result.success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Audius host $host failed: ${e.message}")
            }
        }
        return Result.failure(lastError ?: IllegalStateException("No Audius host reachable"))
    }

    private fun AudiusTrackDto.toDomainTrack(host: String, dataSaver: Boolean): Track? {
        if (id.isBlank() || title.isBlank()) return null
        val streamable = isStreamable != false
        val streamUrl = if (streamable) {
            "${host.trimEnd('/')}/v1/tracks/$id/stream?app_name=MasterWave"
        } else ""
        val permalinkUrl = if (!permalink.isNullOrBlank()) {
            if (permalink.startsWith("http")) permalink else "https://audius.co$permalink"
        } else ""
        val cap = when {
            streamUrl.isNotBlank() -> PlaybackCapability.FULL_STREAM
            permalinkUrl.isNotBlank() -> PlaybackCapability.EXTERNAL_LINK
            else -> PlaybackCapability.METADATA_ONLY
        }
        return Track(
            key = "audius_$id",
            source = MusicSource.AUDIUS,
            title = title,
            artist = user?.name?.ifBlank { user.handle } ?: "Independent Artist",
            album = mood?.takeIf { it.isNotBlank() } ?: "Single",
            artworkUrl = artwork?.best(dataSaver).orEmpty(),
            streamUrl = streamUrl,
            externalUrl = permalinkUrl,
            durationMs = duration * 1000L,
            capability = cap,
            genre = genre.orEmpty(),
            playCount = playCount
        )
    }

    private fun AudiusPlaylistDto.toDomainPlaylist(host: String, dataSaver: Boolean): CuratedPlaylist {
        val domainTracks = tracks.orEmpty().mapNotNull { it.toDomainTrack(host, dataSaver) }
        val art = artwork?.best(dataSaver)?.ifBlank { null }
            ?: domainTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
            ?: ""
        return CuratedPlaylist(
            key = "audius_pl_$id",
            source = MusicSource.AUDIUS,
            title = playlistName.ifBlank { "Curated Wave" },
            subtitle = description?.takeIf { it.isNotBlank() }
                ?: "Curated by ${user?.name ?: "Audius"} • ${domainTracks.size} tracks",
            artworkUrl = art,
            externalUrl = permalink?.let { if (it.startsWith("http")) it else "https://audius.co$it" }.orEmpty(),
            tracks = domainTracks,
            trackCount = domainTracks.size
        )
    }

    private fun ItunesItemDto.toDomainTrack(dataSaver: Boolean): Track? {
        val tid = trackId ?: return null
        val name = trackName?.takeIf { it.isNotBlank() } ?: return null
        val preview = previewUrl.orEmpty()
        val viewUrl = trackViewUrl ?: collectionViewUrl.orEmpty()
        val cap = when {
            preview.isNotBlank() -> PlaybackCapability.PREVIEW_30S
            viewUrl.isNotBlank() -> PlaybackCapability.EXTERNAL_LINK
            else -> PlaybackCapability.METADATA_ONLY
        }
        return Track(
            key = "itunes_track_$tid",
            source = MusicSource.ITUNES,
            title = name,
            artist = artistName.orEmpty().ifBlank { "Unknown Artist" },
            album = collectionName.orEmpty(),
            artworkUrl = highResArtwork(dataSaver),
            streamUrl = preview,
            externalUrl = viewUrl,
            durationMs = if (preview.isNotBlank()) 30_000L else (trackTimeMillis ?: 0L),
            capability = cap,
            genre = primaryGenreName.orEmpty()
        )
    }

    private fun ItunesItemDto.toDomainAlbum(dataSaver: Boolean): Album? {
        val cid = collectionId ?: return null
        val name = collectionName?.takeIf { it.isNotBlank() } ?: return null
        return Album(
            key = "itunes_album_$cid",
            source = MusicSource.ITUNES,
            title = name,
            artist = artistName.orEmpty().ifBlank { "Various Artists" },
            artworkUrl = highResArtwork(dataSaver),
            externalUrl = collectionViewUrl.orEmpty(),
            releaseYear = releaseDate?.take(4).orEmpty(),
            trackCount = trackCount ?: 0,
            genre = primaryGenreName.orEmpty()
        )
    }

    private fun ItunesItemDto.toDomainArtist(dataSaver: Boolean): Artist? {
        val aid = artistId ?: return null
        val name = artistName?.takeIf { it.isNotBlank() } ?: return null
        return Artist(
            key = "itunes_artist_$aid",
            source = MusicSource.ITUNES,
            name = name,
            artworkUrl = highResArtwork(dataSaver),
            genre = primaryGenreName.orEmpty(),
            externalUrl = artistLinkUrl.orEmpty()
        )
    }

    private fun interleave(first: List<Track>, second: List<Track>): List<Track> {
        val out = mutableListOf<Track>()
        val maxLen = maxOf(first.size, second.size)
        for (i in 0 until maxLen) {
            if (i < first.size) out += first[i]
            if (i < second.size) out += second[i]
        }
        return out.distinctBy { it.key }
    }

    private companion object {
        const val TAG = "MusicCatalogRepo"
        const val FEED_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
