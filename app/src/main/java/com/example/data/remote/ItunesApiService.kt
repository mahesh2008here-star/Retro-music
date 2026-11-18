package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesItemDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val artistId: Long? = null,
    val collectionId: Long? = null,
    val trackId: Long? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val trackName: String? = null,
    val collectionViewUrl: String? = null,
    val trackViewUrl: String? = null,
    val artistLinkUrl: String? = null,
    val previewUrl: String? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null,
    val trackCount: Int? = null,
    val trackTimeMillis: Long? = null,
    val primaryGenreName: String? = null
) {
    fun highResArtwork(dataSaver: Boolean = false): String {
        val raw = artworkUrl100.orEmpty()
        if (raw.isBlank()) return ""
        val size = if (dataSaver) "300x300bb" else "600x600bb"
        return raw.replace("100x100bb", size)
    }
}

interface ItunesApiService {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25,
        @Query("country") country: String = "US"
    ): ItunesSearchResponse

    @GET("lookup")
    suspend fun lookupAlbumTracks(
        @Query("id") collectionId: Long,
        @Query("entity") entity: String = "song"
    ): ItunesSearchResponse

    @GET("lookup")
    suspend fun lookupArtistSongs(
        @Query("id") artistId: Long,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 20
    ): ItunesSearchResponse
}
