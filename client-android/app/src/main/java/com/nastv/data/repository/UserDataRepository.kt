package com.nastv.data.repository

import com.nastv.data.api.AddFavoriteRequest
import com.nastv.data.api.PlayProgress
import com.nastv.data.api.UpdatePlayProgressRequest
import com.nastv.data.api.UserDataApi
import com.nastv.data.model.CachedEpisode
import com.nastv.data.model.CachedVideo
import com.nastv.data.model.FavoriteItem
import com.nastv.data.model.PlayHistory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val userDataApi: UserDataApi
) {

    suspend fun getFavorites(): Result<Map<String, FavoriteItem>> {
        return safeApiCall {
            val response = userDataApi.getFavorites()
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get favorites")
            }
        }
    }

    suspend fun addFavorite(sourceId: String, videoId: String, name: String? = null, pic: String? = null, remark: String? = null): Result<Unit> {
        return safeApiCall {
            val response = userDataApi.addFavorite(
                AddFavoriteRequest(sourceId, videoId, name, pic, remark)
            )
            if (response.isSuccess) {
                Unit
            } else {
                throw ApiException(response.message ?: "Failed to add favorite")
            }
        }
    }

    suspend fun removeFavorite(sourceId: String, videoId: String): Result<Unit> {
        return safeApiCall {
            val response = userDataApi.removeFavorite(sourceId, videoId)
            if (response.isSuccess) {
                Unit
            } else {
                throw ApiException(response.message ?: "Failed to remove favorite")
            }
        }
    }

    suspend fun getPlayHistory(): Result<List<PlayHistory>> {
        return safeApiCall {
            val response = userDataApi.getPlayHistory()
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get play history")
            }
        }
    }

    suspend fun deletePlayHistory(sourceId: String, videoId: String): Result<Unit> {
        return safeApiCall {
            val response = userDataApi.deletePlayHistory(sourceId, videoId)
            if (response.isSuccess) {
                Unit
            } else {
                throw ApiException(response.message ?: "Failed to delete play history")
            }
        }
    }

    suspend fun updatePlayProgress(
        sourceId: String,
        videoId: String,
        progress: Float,
        episode: String,
        name: String,
        pic: String? = null,
        duration: Float? = null,
        totalEpisodes: Int? = null,
        activeSource: Int? = null,
        activeEpisodeIndex: Int? = null
    ): Result<Unit> {
        return safeApiCall {
            val response = userDataApi.updatePlayProgress(
                UpdatePlayProgressRequest(
                    sourceId, videoId, progress, episode, name,
                    pic, duration, totalEpisodes, activeSource, activeEpisodeIndex
                )
            )
            if (response.isSuccess) {
                Unit
            } else {
                throw ApiException(response.message ?: "Failed to update play progress")
            }
        }
    }

    suspend fun getPlayProgress(sourceId: String, videoId: String): Result<PlayProgress> {
        return safeApiCall {
            val response = userDataApi.getPlayProgress(sourceId, videoId)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get play progress")
            }
        }
    }

    suspend fun getCachedEpisodes(): Result<List<CachedEpisode>> {
        return safeApiCall {
            val response = userDataApi.getCachedEpisodes()
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get cached episodes")
            }
        }
    }

    suspend fun getCachedVideos(): Result<List<CachedVideo>> {
        return safeApiCall {
            val response = userDataApi.getCachedVideos()
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Failed to get cached videos")
            }
        }
    }
}
