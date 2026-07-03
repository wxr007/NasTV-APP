package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Video(
    @Json(name = "id")
    val id: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "pic")
    val pic: String? = null,

    @Json(name = "year")
    val year: String? = null,

    @Json(name = "area")
    val area: String? = null,

    @Json(name = "type")
    val type: String? = null,

    @Json(name = "remark")
    val remark: String? = null,

    @Json(name = "sourceId")
    val sourceId: String
)

@JsonClass(generateAdapter = true)
data class VideoListResponse(
    @Json(name = "videos")
    val videos: List<Video> = emptyList(),

    @Json(name = "total")
    val total: Int = 0
)
