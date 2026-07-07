package com.nastv.data.repository

import android.util.Log
import com.nastv.data.api.HealthCheckApi
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Server
import com.nastv.di.HealthCheckClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @HealthCheckClient private val healthCheckClient: OkHttpClient
) {
    val serversFlow: Flow<List<Server>> = preferencesRepository.serversFlow

    suspend fun checkServerHealth(url: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = normalizeUrl(url)
                Log.d("ServerRepository", "开始健康检查: $normalizedUrl")
                
                val request = Request.Builder()
                    .url(normalizedUrl)
                    .get()
                    .build()
                
                Log.d("ServerRepository", "发送请求: ${request.url}")
                
                healthCheckClient.newCall(request).execute().use { response ->
                    Log.d("ServerRepository", "收到响应: code=${response.code}, message=${response.message}")
                    Log.d("ServerRepository", "响应头: ${response.headers}")
                    
                    val body = response.body?.string()
                    Log.d("ServerRepository", "响应体: ${body?.take(200)}")
                    
                    // 只要服务器有响应（即使是404），说明服务器是可访问的
                    val success = response.isSuccessful || response.code in 400..499
                    Log.d("ServerRepository", "健康检查结果: $success")
                    Result.Success(success)
                }
            } catch (e: Exception) {
                Log.e("ServerRepository", "健康检查失败", e)
                Result.Error(e)
            }
        }
    }

    suspend fun addServer(name: String, url: String): Result<Server> {
        return try {
            val normalizedUrl = normalizeUrl(url)
            val server = Server(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { normalizedUrl },
                url = normalizedUrl,
                lastUsed = 0
            )
            preferencesRepository.saveServer(server)
            Result.Success(server)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun removeServer(serverId: String) {
        preferencesRepository.deleteServer(serverId)
    }

    suspend fun getServerById(serverId: String): Server? {
        return preferencesRepository.serversFlow.first().find { it.id == serverId }
    }

    suspend fun updateLastUsed(serverId: String) {
        preferencesRepository.updateLastUsed(serverId, null)
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().removeSuffix("/")
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}
