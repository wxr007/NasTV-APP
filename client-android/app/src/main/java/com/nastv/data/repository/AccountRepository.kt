package com.nastv.data.repository

import com.nastv.data.api.AuthApi
import com.nastv.data.api.LoginRequest
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Account
import com.nastv.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authApi: AuthApi
) {
    val accountsFlow: Flow<List<Account>> = preferencesRepository.accountsFlow

    fun getAccountsForServer(serverId: String): Flow<List<Account>> {
        return accountsFlow.map { accounts ->
            accounts.filter { it.serverId == serverId }
        }
    }

    suspend fun addAccount(serverId: String, username: String, password: String): Result<Account> {
        return try {
            val account = Account(
                id = UUID.randomUUID().toString(),
                serverId = serverId,
                username = username,
                password = password,
                lastUsed = 0
            )
            preferencesRepository.saveAccount(account)
            Result.Success(account)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun removeAccount(accountId: String) {
        preferencesRepository.deleteAccount(accountId)
    }

    suspend fun loginWithAccount(account: Account): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(account.username, account.password))
            if (response.isSuccess && response.data != null) {
                val user = response.data
                preferencesRepository.saveAuthToken(user.token)
                preferencesRepository.saveUsername(user.username)
                preferencesRepository.updateLastUsed(account.serverId, account.id)
                Result.Success(user)
            } else {
                Result.Error(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAccountById(accountId: String): Account? {
        return preferencesRepository.accountsFlow.first().find { it.id == accountId }
    }
}
