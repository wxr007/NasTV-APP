package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 已缓存视频系列（按 sourceId+videoId 分组）
 * 对应 GET /api/cache/videos 的响应项
 */
@JsonClass(generateAdapter = true)
data class CachedVideo(
    @Json(name = "sourceId")
    val sourceId: String,

    @Json(name = "videoId")
    val videoId: String,

    @Json(name = "title")
    val title: String,

    @Json(name = "picUrl")
    val picUrl: String? = null,

    @Json(name = "cachedCount")
    val cachedCount: Int = 0,

    @Json(name = "updatedAt")
    val updatedAt: String? = null
)
