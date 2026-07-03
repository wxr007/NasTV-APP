package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoDetail(
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

    @Json(name = "actor")
    val actor: String? = null,

    @Json(name = "director")
    val director: String? = null,

    @Json(name = "content")
    val content: String? = null,

    @Json(name = "playFrom")
    val playFrom: List<String> = emptyList(),

    @Json(name = "episodes")
    val episodes: List<List<Episode>> = emptyList(),

    @Json(name = "sourceId")
    val sourceId: String
) {
    /** Returns list of (index, name) pairs for play lines. */
    val playWithIndex: List<Pair<Int, String>>
        get() = playFrom.mapIndexed { index, name -> index to name }
}

@JsonClass(generateAdapter = true)
data class Episode(
    @Json(name = "name")
    val name: String,

    @Json(name = "url")
    val url: String
)
