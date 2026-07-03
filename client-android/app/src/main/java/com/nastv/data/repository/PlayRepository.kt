package com.nastv.data.repository

import com.nastv.data.api.PlayApi
import com.nastv.data.model.CacheStatusResponse
import com.nastv.data.model.VideoDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayRepository @Inject constructor(
    private val playApi: PlayApi
) {

    suspend fun getVideoDetail(sourceId: String, videoId: String): Result<VideoDetail> {
        return safeApiCall {
            val response = playApi.getVideoDetail(sourceId, videoId)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get video detail")
            }
        }
    }

    suspend fun proxyM3u8(url: String): Result<String> {
        return safeApiCall {
            val response = playApi.proxyM3u8(url)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to proxy m3u8")
            }
        }
    }

    suspend fun getCacheStatus(sourceId: String, videoId: String): Result<CacheStatusResponse> {
        return safeApiCall {
            val response = playApi.getCacheStatus(sourceId, videoId)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get cache status")
            }
        }
    }
}
