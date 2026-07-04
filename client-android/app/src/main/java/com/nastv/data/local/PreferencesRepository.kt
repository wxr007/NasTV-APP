package com.nastv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nastv.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey(Constants.PREF_SERVER_URL)
        val AUTH_TOKEN_KEY = stringPreferencesKey(Constants.PREF_AUTH_TOKEN)
        val USERNAME_KEY = stringPreferencesKey(Constants.PREF_USERNAME)
        val USE_IMAGE_PROXY_KEY = booleanPreferencesKey(Constants.PREF_USE_IMAGE_PROXY)
        val SEEK_DURATION_KEY = intPreferencesKey(Constants.PREF_SEEK_DURATION)
        val FORCE_TV_MODE_KEY = booleanPreferencesKey(Constants.PREF_FORCE_TV_MODE)
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

    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USERNAME_KEY)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
