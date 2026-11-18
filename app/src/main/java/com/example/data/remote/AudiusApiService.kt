package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AudiusDiscoveryNodesResponse(
    val data: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusTracksResponse(
    val data: List<AudiusTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistsResponse(
    val data: List<AudiusPlaylistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusUsersResponse(
    val data: List<AudiusUserDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusArtworkDto(
    @Json(name = "150x150") val x150: String? = null,
    @Json(name = "480x480") val x480: String? = null,
    @Json(name = "1000x1000") val x1000: String? = null
) {
    fun best(dataSaver: Boolean = false): String =
        if (dataSaver) (x480 ?: x150 ?: x1000).orEmpty()
        else (x1000 ?: x480 ?: x150).orEmpty()
}

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    @Json(name = "profile_picture") val profilePicture: AudiusArtworkDto? = null,
    @Json(name = "follower_count") val followerCount: Long = 0L,
    val bio: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    val id: String = "",
    val title: String = "",
    val duration: Long = 0L,
    val genre: String? = null,
    val mood: String? = null,
    val permalink: String? = null,
    @Json(name = "play_count") val playCount: Long = 0L,
    @Json(name = "is_streamable") val isStreamable: Boolean? = true,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistDto(
    val id: String = "",
    @Json(name = "playlist_name") val playlistName: String = "",
    val description: String? = null,
    val permalink: String? = null,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null,
    val tracks: List<AudiusTrackDto>? = null
)

interface AudiusBootstrapApi {
    @GET("/")
    suspend fun getDiscoveryNodes(): AudiusDiscoveryNodesResponse
}

interface AudiusApiService {
    @GET("v1/tracks/trending")
    suspend fun getTrendingTracks(
        @Query("time") time: String = "week",
        @Query("genre") genre: String? = null,
        @Query("limit") limit: Int = 25,
        @Query("app_name") appName: String = "MasterWave"
    ): AudiusTracksResponse

    @GET("v1/tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("limit") limit: Int = 25,
        @Query("app_name") appName: String = "MasterWave"
    ): AudiusTracksResponse

    @GET("v1/playlists/trending")
    suspend fun getTrendingPlaylists(
        @Query("time") time: String = "week",
        @Query("limit") limit: Int = 12,
        @Query("app_name") appName: String = "MasterWave"
    ): AudiusPlaylistsResponse

    @GET("v1/users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("limit") limit: Int = 15,
        @Query("app_name") appName: String = "MasterWave"
    ): AudiusUsersResponse
}
