package com.nastv.data.repository

import com.nastv.data.api.AuthApi
import com.nastv.data.api.LoginRequest
import com.nastv.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed class representing the result of a repository operation.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }
}

/**
 * Extension function to safely execute a suspend function and wrap the result.
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        val response = apiCall()
        Result.Success(response)
    } catch (e: Exception) {
        Result.Error(e)
    }
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {

    suspend fun login(username: String, password: String): Result<User> {
        return safeApiCall {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw ApiException(response.message ?: "Login failed")
            }
        }
    }
}

/**
 * Custom exception for API errors.
 */
class ApiException(message: String) : Exception(message)
