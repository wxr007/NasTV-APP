package com.nastv.data.repository

import com.nastv.data.api.SearchApi
import com.nastv.data.model.Video
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

    suspend fun search(keyword: String): Result<List<Video>> {
        return safeApiCall {
            val response = searchApi.search(keyword)
            if (response.isSuccess && response.data != null) {
                response.data.videos
            } else {
                throw ApiException(response.message ?: "Search failed")
            }
        }
    }

    suspend fun getSuggestions(keyword: String): Result<List<String>> {
        return safeApiCall {
            val response = searchApi.getSuggestions(keyword)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get suggestions")
            }
        }
    }
}
