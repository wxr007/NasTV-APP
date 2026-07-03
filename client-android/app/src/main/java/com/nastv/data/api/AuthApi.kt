package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.User
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

data class LoginRequest(
    val username: String,
    val password: String
)
