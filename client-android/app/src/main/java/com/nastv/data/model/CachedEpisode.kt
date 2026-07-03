package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CachedEpisode(
    @Json(name = "sourceId")
    val sourceId: String,

    @Json(name = "videoId")
    val videoId: String,

    @Json(name = "episodeIndex")
    val episodeIndex: Int,

    @Json(name = "diskSize")
    val diskSize: Long,

    @Json(name = "status")
    val status: CacheStatus?
)

@JsonClass(generateAdapter = true)
data class CacheStatus(
    @Json(name = "state")
    val state: String,

    @Json(name = "progress")
    val progress: Int,

    @Json(name = "total")
    val total: Int,

    @Json(name = "cached")
    val cached: Int,

    @Json(name = "totalBytes")
    val totalBytes: Long,

    @Json(name = "cachedBytes")
    val cachedBytes: Long,

    @Json(name = "updatedAt")
    val updatedAt: String? = null,

    @Json(name = "title")
    val title: String? = null,

    @Json(name = "episodeUrl")
    val episodeUrl: String? = null,

    @Json(name = "picUrl")
    val picUrl: String? = null,

    @Json(name = "error")
    val error: String? = null
)
