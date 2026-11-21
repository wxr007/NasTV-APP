package com.nastv.data.api

import com.nastv.data.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    /**
     * GET /api/search/suggest?keyword=
     * Get search suggestions.
     *
     * 注：客户端的搜索已改为按站源并发直连（SearchViewModel），不再调用 /api/search 聚合接口。
     */
    @GET("api/search/suggest")
    suspend fun getSuggestions(
        @Query("keyword") keyword: String
    ): ApiResponse<List<String>>
}
