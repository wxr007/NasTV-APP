package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.CachedEpisode
import com.nastv.data.model.CachedVideo
import com.nastv.data.model.FavoriteItem
import com.nastv.data.model.PlayHistory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface UserDataApi {

    /**
     * GET /api/favorites
     * Get user's favorites list (auth required).
     * Returns { key: { name, pic, remark } } map format.
     */
    @GET("api/favorites")
    suspend fun getFavorites(): ApiResponse<Map<String, FavoriteItem>>

    /**
     * POST /api/favorites
     * Add a video to favorites (auth required).
     * Request body: { "sourceId": "...", "videoId": "...", "name": "...", "pic": "...", "remark": "..." }
     */
    @POST("api/favorites")
    suspend fun addFavorite(
        @Body request: AddFavoriteRequest
    ): ApiResponse<Map<String, Any?>>

    /**
     * DELETE /api/favorites/:sourceId/:videoId
     * Remove a video from favorites (auth required).
     */
    @HTTP(method = "DELETE", path = "api/favorites/{sourceId}/{videoId}", hasBody = true)
    suspend fun removeFavorite(
        @Path("sourceId") sourceId: String,
        @Path("videoId") videoId: String
    ): ApiResponse<Map<String, Any?>>

    /**
     * GET /api/play-history
     * Get user's play history (auth required).
     */
    @GET("api/play-history")
    suspend fun getPlayHistory(): ApiResponse<List<PlayHistory>>

    /**
     * PUT /api/play-history
     * Update play progress (auth required).
     * Request body: JSON with sourceId, videoId, progress, episode, name, pic, duration, totalEpisodes, activeSource, activeEpisodeIndex
     */
    @PUT("api/play-history")
    suspend fun updatePlayProgress(
        @Body request: UpdatePlayProgressRequest
    ): ApiResponse<Map<String, Any?>>

    /**
     * DELETE /api/play-history/:sourceId/:videoId
     * Remove a video from play history (auth required).
     */
    @HTTP(method = "DELETE", path = "api/play-history/{sourceId}/{videoId}", hasBody = true)
    suspend fun deletePlayHistory(
        @Path("sourceId") sourceId: String,
        @Path("videoId") videoId: String
    ): ApiResponse<Map<String, Any?>>

    /**
     * GET /api/play-progress/:sourceId/:videoId
     * Get play progress for a specific video (auth required).
     */
    @GET("api/play-progress/{sourceId}/{videoId}")
    suspend fun getPlayProgress(
        @Path("sourceId") sourceId: String,
        @Path("videoId") videoId: String
    ): ApiResponse<PlayProgress>

    /**
     * GET /api/cache/episodes
     * Get all cached episodes (auth required).
     */
    @GET("api/cache/episodes")
    suspend fun getCachedEpisodes(): ApiResponse<List<CachedEpisode>>

    /**
     * GET /api/cache/videos
     * Get all cached video series grouped by sourceId+videoId (auth required).
     */
    @GET("api/cache/videos")
    suspend fun getCachedVideos(): ApiResponse<List<CachedVideo>>
}

@JsonClass(generateAdapter = true)
data class AddFavoriteRequest(
    @Json(name = "sourceId")
    val sourceId: String,

    @Json(name = "videoId")
    val videoId: String,

    @Json(name = "name")
    val name: String? = null,

    @Json(name = "pic")
    val pic: String? = null,

    @Json(name = "remark")
    val remark: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdatePlayProgressRequest(
    @Json(name = "sourceId")
    val sourceId: String,

    @Json(name = "videoId")
    val videoId: String,

    @Json(name = "progress")
    val progress: Float,

    @Json(name = "episode")
    val episode: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "pic")
    val pic: String? = null,

    @Json(name = "duration")
    val duration: Float? = null,

    @Json(name = "totalEpisodes")
    val totalEpisodes: Int? = null,

    @Json(name = "activeSource")
    val activeSource: Int? = null,

    @Json(name = "activeEpisodeIndex")
    val activeEpisodeIndex: Int? = null
)

/**
 * Play progress data returned from the API.
 */
@JsonClass(generateAdapter = true)
data class PlayProgress(
    @Json(name = "progress")
    val progress: Float? = null,

    @Json(name = "duration")
    val duration: Float? = null,

    @Json(name = "episode")
    val episode: String? = null,

    @Json(name = "totalEpisodes")
    val totalEpisodes: Int? = null,

    @Json(name = "activeSource")
    val activeSource: Int? = null,

    @Json(name = "activeEpisodeIndex")
    val activeEpisodeIndex: Int? = null,

    @Json(name = "name")
    val name: String? = null,

    @Json(name = "pic")
    val pic: String? = null
)
