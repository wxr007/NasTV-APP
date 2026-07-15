package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.CacheStatusResponse
import com.nastv.data.model.VideoDetail
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = false)
data class ResolveResponse(val url: String)

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
     * GET /api/play/resolve?url=
     * Resolve playback URL to real m3u8 address.
     */
    @GET("api/play/resolve")
    suspend fun resolvePlayUrl(
        @Query("url") url: String
    ): ApiResponse<ResolveResponse>

    /**
     * GET /api/cache/status/:sourceId/:videoId
     * Get cache status for a video.
     */
    @GET("api/cache/status/{sourceId}/{videoId}")
    suspend fun getCacheStatus(
        @Path("sourceId") sourceId: String,
        @Path("videoId") videoId: String
    ): ApiResponse<CacheStatusResponse>

    /**
     * GET /api/adblock/strings
     * 获取所有广告屏蔽字符串及其备注，用于播放页的广告分片过滤。
     */
    @GET("api/adblock/strings")
    suspend fun getAdBlockStrings(): AdBlockStringsResponse
}

data class AdBlockStringsResponse(
    @Json(name = "code")
    val code: Int = 0,

    @Json(name = "data")
    val data: List<AdBlockString>? = null,

    @Json(name = "message")
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 0
}

data class AdBlockString(
    @Json(name = "string")
    val string: String = "",

    @Json(name = "note")
    val note: String? = null
)
