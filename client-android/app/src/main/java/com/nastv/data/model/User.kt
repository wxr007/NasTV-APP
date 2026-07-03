package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "token")
    val token: String,

    @Json(name = "expiresIn")
    val expiresIn: Long,

    @Json(name = "username")
    val username: String,

    @Json(name = "role")
    val role: String? = null
)
