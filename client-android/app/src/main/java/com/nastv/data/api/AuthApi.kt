package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    /**
     * POST /api/auth/token
     * Login with username and password, returns token info.
     */
    @POST("api/auth/token")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<User>
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)
