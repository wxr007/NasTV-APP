package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CacheStatusResponse(
    @Json(name = "episodes")
    val episodes: Map<String, CacheStatus> = emptyMap()
)
