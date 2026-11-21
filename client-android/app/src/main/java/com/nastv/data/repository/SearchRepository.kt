package com.nastv.data.repository

import com.nastv.data.api.SearchApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索相关接口。搜索本身已改为客户端按站源并发直连（见 SearchViewModel），
 * 这里只剩联想词。
 */
@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

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
