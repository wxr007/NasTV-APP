package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.Category
import com.nastv.data.model.Video
import com.nastv.data.model.VideoListResponse
import com.nastv.data.model.VideoSource
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SourceApi {

    /**
     * GET /api/sources
     * Get the list of available sources.
     */
    @GET("api/sources")
    suspend fun getSources(): ApiResponse<List<VideoSource>>

    /**
     * GET /api/source/:sourceId/categories
     * Get categories for a specific source.
     */
    @GET("api/source/{sourceId}/categories")
    suspend fun getCategories(
        @Path("sourceId") sourceId: String
    ): ApiResponse<List<Category>>

    /**
     * GET /api/source/:sourceId/videos?page=&category=&keyword=
     * Get videos from a specific source with optional filters.
     */
    @GET("api/source/{sourceId}/videos")
    suspend fun getVideos(
        @Path("sourceId") sourceId: String,
        @Query("page") page: Int? = null,
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<VideoListResponse>
}
