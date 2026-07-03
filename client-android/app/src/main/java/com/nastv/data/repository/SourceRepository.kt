package com.nastv.data.repository

import com.nastv.data.api.SourceApi
import com.nastv.data.model.Category
import com.nastv.data.model.Video
import com.nastv.data.model.VideoSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepository @Inject constructor(
    private val sourceApi: SourceApi
) {

    suspend fun getSources(): Result<List<VideoSource>> {
        return safeApiCall {
            val response = sourceApi.getSources()
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get sources")
            }
        }
    }

    suspend fun getCategories(sourceId: String): Result<List<Category>> {
        return safeApiCall {
            val response = sourceApi.getCategories(sourceId)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get categories")
            }
        }
    }

    suspend fun getVideos(
        sourceId: String,
        page: Int? = null,
        category: String? = null,
        keyword: String? = null
    ): Result<List<Video>> {
        return safeApiCall {
            val response = sourceApi.getVideos(sourceId, page, category, keyword)
            if (response.isSuccess && response.data != null) {
                response.data.videos
            } else {
                throw ApiException(response.message ?: "Failed to get videos")
            }
        }
    }
}
