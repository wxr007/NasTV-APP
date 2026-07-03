package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FavoriteItem(
    @Json(name = "name")
    val name: String,

    @Json(name = "pic")
    val pic: String? = null,

    @Json(name = "remark")
    val remark: String? = null
)
