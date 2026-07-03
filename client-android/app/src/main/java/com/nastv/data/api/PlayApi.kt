package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.CacheStatusResponse
import com.nastv.data.model.VideoDetail
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlayApi {

    /**
     * GET /api/play/detail?sourceId=&id=
     * Get video detail with episodes.
     */
    @GET("api/play/detail")
    suspend fun getVideoDetail(
        @Query("sourceId") sourceId: String,
        @Query("id") videoId: String
    ): ApiResponse<VideoDetail>

    /**
     * GET /api/play/m3u8?url=
     * Proxy m3u8 playlist through the backend.
     */
    @GET("api/play/m3u8")
    suspend fun proxyM3u8(
        @Query("url") url: String
    ): ApiResponse<String>

    /**
     * GET /api/cache/status/:sourceId/:videoId
     * Get cache status for a video.
     */
    @GET("api/cache/status/{sourceId}/{videoId}")
    suspend fun getCacheStatus(
        @Path("sourceId") sourceId: String,
        @Path("videoId") videoId: String
    ): ApiResponse<CacheStatusResponse>
}
