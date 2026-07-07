package com.nastv.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Account(
    val id: String,
    val serverId: String,
    val username: String,
    val password: String,
    val lastUsed: Long = 0
)
