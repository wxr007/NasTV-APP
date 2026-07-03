package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import com.nastv.data.model.Video
import com.nastv.data.model.VideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    /**
     * GET /api/search?keyword=
     * Aggregate search across all sources.
     */
    @GET("api/search")
    suspend fun search(
        @Query("keyword") keyword: String
    ): ApiResponse<VideoListResponse>

    /**
     * GET /api/search/suggest?keyword=
     * Get search suggestions.
     */
    @GET("api/search/suggest")
    suspend fun getSuggestions(
        @Query("keyword") keyword: String
    ): ApiResponse<List<String>>
}
