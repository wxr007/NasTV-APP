package com.nastv.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Server(
    val id: String,
    val name: String,
    val url: String,
    val lastUsed: Long = 0
)
