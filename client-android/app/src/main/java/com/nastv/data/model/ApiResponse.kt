package com.nastv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic API response wrapper matching backend format:
 * { "code": 0, "data": ..., "message": "..." }
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "code")
    val code: Int,

    @Json(name = "data")
    val data: T?,

    @Json(name = "message")
    val message: String? = null
) {
    /**
     * Returns true if the response indicates success (code == 0).
     */
    val isSuccess: Boolean
        get() = code == 0
}
