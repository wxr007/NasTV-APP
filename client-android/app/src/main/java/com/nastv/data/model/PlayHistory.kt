package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayHistory(
    @Json(name = "key")
    val key: String,

    @Json(name = "sourceId")
    val sourceId: String,

    @Json(name = "videoId")
    val videoId: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "pic")
    val pic: String? = null,

    @Json(name = "progress")
    val progress: Float = 0f,

    @Json(name = "duration")
    val duration: Float? = null,

    @Json(name = "timestamp")
    val timestamp: Long = 0,

    @Json(name = "episode")
    val episode: String? = null,

    @Json(name = "totalEpisodes")
    val totalEpisodes: Int? = null,

    @Json(name = "activeSource")
    val activeSource: Int? = null,

    @Json(name = "activeEpisodeIndex")
    val activeEpisodeIndex: Int? = null
)
