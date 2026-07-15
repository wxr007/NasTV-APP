package com.nastv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.utils.ImageUrlHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _useImageProxy = MutableStateFlow(true)
    val useImageProxy: StateFlow<Boolean> = _useImageProxy.asStateFlow()

    private val _seekDuration = MutableStateFlow(10)
    val seekDuration: StateFlow<Int> = _seekDuration.asStateFlow()

    private val _forceTvMode = MutableStateFlow(false)
    val forceTvMode: StateFlow<Boolean> = _forceTvMode.asStateFlow()

    private val _themeMode = MutableStateFlow("system") // "system", "dark", "light"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _adBlockEnabled = MutableStateFlow(false)
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _serverName = MutableStateFlow<String?>(null)
    val serverName: StateFlow<String?> = _serverName.asStateFlow()

    init {
        viewModelScope.launch {
            _useImageProxy.value = preferencesRepository.useImageProxyFlow.first()
            _seekDuration.value = preferencesRepository.seekDurationFlow.first()
            _forceTvMode.value = preferencesRepository.forceTvModeFlow.first()
            _themeMode.value = preferencesRepository.themeModeFlow.first()
            _username.value = preferencesRepository.usernameFlow.first()
            _userRole.value = preferencesRepository.userRoleFlow.first()
            
            // 获取当前登录的服务器名称
            val lastServerId = preferencesRepository.lastServerIdFlow.first()
            if (lastServerId != null) {
                val servers = preferencesRepository.serversFlow.first()
                _serverName.value = servers.find { it.id == lastServerId }?.name
            }
        }
    }

    suspend fun getServerUrl(): String {
        return preferencesRepository.getServerUrl()
    }

    fun saveServerUrl(url: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.saveServerUrl(url)
            onComplete()
        }
    }

    fun setUseImageProxy(useProxy: Boolean) {
        _useImageProxy.value = useProxy
        ImageUrlHelper.setUseImageProxy(useProxy)
        viewModelScope.launch {
            preferencesRepository.saveUseImageProxy(useProxy)
        }
    }

    fun setSeekDuration(seconds: Int) {
        _seekDuration.value = seconds
        viewModelScope.launch {
            preferencesRepository.saveSeekDuration(seconds)
        }
    }

    fun setForceTvMode(force: Boolean) {
        _forceTvMode.value = force
        viewModelScope.launch {
            preferencesRepository.saveForceTvMode(force)
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            preferencesRepository.saveThemeMode(mode)
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        _adBlockEnabled.value = enabled
        viewModelScope.launch {
            preferencesRepository.saveAdBlockEnabled(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesRepository.clearAuth()
        }
    }
}
