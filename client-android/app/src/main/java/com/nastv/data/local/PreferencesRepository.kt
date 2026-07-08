package com.nastv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nastv.data.model.Account
import com.nastv.data.model.Server
import com.nastv.utils.Constants
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi
) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey(Constants.PREF_SERVER_URL)
        val AUTH_TOKEN_KEY = stringPreferencesKey(Constants.PREF_AUTH_TOKEN)
        val USERNAME_KEY = stringPreferencesKey(Constants.PREF_USERNAME)
        val USE_IMAGE_PROXY_KEY = booleanPreferencesKey(Constants.PREF_USE_IMAGE_PROXY)
        val SEEK_DURATION_KEY = intPreferencesKey(Constants.PREF_SEEK_DURATION)
        val FORCE_TV_MODE_KEY = booleanPreferencesKey(Constants.PREF_FORCE_TV_MODE)
        val THEME_MODE_KEY = stringPreferencesKey(Constants.PREF_THEME_MODE)
        val USER_ROLE_KEY = stringPreferencesKey(Constants.PREF_USER_ROLE)
        val BUFFER_MIN_DURATION_KEY = intPreferencesKey(Constants.PREF_BUFFER_MIN_DURATION)
        val BUFFER_MAX_DURATION_KEY = intPreferencesKey(Constants.PREF_BUFFER_MAX_DURATION)
        
        // 新增：服务器和账号存储
        val SERVERS_KEY = stringPreferencesKey("servers_json")
        val ACCOUNTS_KEY = stringPreferencesKey("accounts_json")
        val LAST_SERVER_ID_KEY = stringPreferencesKey("last_server_id")
        val LAST_ACCOUNT_ID_KEY = stringPreferencesKey("last_account_id")
    }

    val serverUrlFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[SERVER_URL_KEY] ?: Constants.DEFAULT_BASE_URL
    }

    val authTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    val usernameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    val useImageProxyFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_IMAGE_PROXY_KEY] ?: true
    }

    val seekDurationFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SEEK_DURATION_KEY] ?: 30
    }

    val forceTvModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FORCE_TV_MODE_KEY] ?: false
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "system" // "system", "dark", "light"
    }

    val userRoleFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ROLE_KEY]
    }

    val bufferMinDurationFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BUFFER_MIN_DURATION_KEY] ?: Constants.DEFAULT_BUFFER_MIN_DURATION
    }

    val bufferMaxDurationFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BUFFER_MAX_DURATION_KEY] ?: Constants.DEFAULT_BUFFER_MAX_DURATION
    }

    suspend fun getServerUrl(): String {
        return dataStore.data.map { preferences ->
            preferences[SERVER_URL_KEY] ?: Constants.DEFAULT_BASE_URL
        }.first()
    }

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun saveUserRole(role: String?) {
        dataStore.edit { preferences ->
            if (role != null) {
                preferences[USER_ROLE_KEY] = role
            } else {
                preferences.remove(USER_ROLE_KEY)
            }
        }
    }

    suspend fun saveUseImageProxy(useProxy: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_IMAGE_PROXY_KEY] = useProxy
        }
    }

    suspend fun saveSeekDuration(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[SEEK_DURATION_KEY] = seconds
        }
    }

    suspend fun saveForceTvMode(force: Boolean) {
        dataStore.edit { preferences ->
            preferences[FORCE_TV_MODE_KEY] = force
        }
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun saveBufferMinDuration(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[BUFFER_MIN_DURATION_KEY] = seconds
        }
    }

    suspend fun saveBufferMaxDuration(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[BUFFER_MAX_DURATION_KEY] = seconds
        }
    }

    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(USER_ROLE_KEY)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ==================== 服务器和账号存储 ====================

    private val serverListAdapter = moshi.adapter<List<Server>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, Server::class.java)
    )
    private val accountListAdapter = moshi.adapter<List<Account>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, Account::class.java)
    )

    val serversFlow: Flow<List<Server>> = dataStore.data.map { preferences ->
        val json = preferences[SERVERS_KEY] ?: "[]"
        serverListAdapter.fromJson(json) ?: emptyList()
    }

    val accountsFlow: Flow<List<Account>> = dataStore.data.map { preferences ->
        val json = preferences[ACCOUNTS_KEY] ?: "[]"
        accountListAdapter.fromJson(json) ?: emptyList()
    }

    val lastServerIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_SERVER_ID_KEY]
    }

    val lastAccountIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_ACCOUNT_ID_KEY]
    }

    suspend fun getAccountsForServer(serverId: String): List<Account> {
        return accountsFlow.first().filter { it.serverId == serverId }
    }

    suspend fun saveServer(server: Server) {
        val servers = serversFlow.first().toMutableList()
        val index = servers.indexOfFirst { it.id == server.id }
        if (index >= 0) {
            servers[index] = server
        } else {
            servers.add(server)
        }
        dataStore.edit { preferences ->
            preferences[SERVERS_KEY] = serverListAdapter.toJson(servers)
        }
    }

    suspend fun deleteServer(serverId: String) {
        val servers = serversFlow.first().filter { it.id != serverId }
        val accounts = accountsFlow.first().filter { it.serverId != serverId }
        dataStore.edit { preferences ->
            preferences[SERVERS_KEY] = serverListAdapter.toJson(servers)
            preferences[ACCOUNTS_KEY] = accountListAdapter.toJson(accounts)
            if (preferences[LAST_SERVER_ID_KEY] == serverId) {
                preferences.remove(LAST_SERVER_ID_KEY)
                preferences.remove(LAST_ACCOUNT_ID_KEY)
            }
        }
    }

    suspend fun saveAccount(account: Account) {
        val accounts = accountsFlow.first().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
        } else {
            accounts.add(account)
        }
        dataStore.edit { preferences ->
            preferences[ACCOUNTS_KEY] = accountListAdapter.toJson(accounts)
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val accounts = accountsFlow.first().filter { it.id != accountId }
        dataStore.edit { preferences ->
            preferences[ACCOUNTS_KEY] = accountListAdapter.toJson(accounts)
            if (preferences[LAST_ACCOUNT_ID_KEY] == accountId) {
                preferences.remove(LAST_ACCOUNT_ID_KEY)
            }
        }
    }

    suspend fun updateLastUsed(serverId: String, accountId: String?) {
        val now = System.currentTimeMillis()
        val servers = serversFlow.first().map {
            if (it.id == serverId) it.copy(lastUsed = now) else it
        }
        dataStore.edit { preferences ->
            preferences[SERVERS_KEY] = serverListAdapter.toJson(servers)
            preferences[LAST_SERVER_ID_KEY] = serverId
            if (accountId != null) {
                preferences[LAST_ACCOUNT_ID_KEY] = accountId
                val accounts = accountsFlow.first().map {
                    if (it.id == accountId) it.copy(lastUsed = now) else it
                }
                preferences[ACCOUNTS_KEY] = accountListAdapter.toJson(accounts)
            }
        }
    }
}
