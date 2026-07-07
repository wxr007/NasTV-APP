package com.nastv.data.api

import retrofit2.http.GET

interface HealthCheckApi {
    /**
     * GET /
     * Health check endpoint, returns "NasTV API is running"
     */
    @GET("/")
    suspend fun healthCheck(): String
}
