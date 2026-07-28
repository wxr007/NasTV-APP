package com.nastv.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Server
import com.nastv.data.repository.Result
import com.nastv.data.repository.ServerRepository
import com.nastv.utils.ImageUrlHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ServerSelectState {
    object Idle : ServerSelectState()
    object Loading : ServerSelectState()
    data class ServerSelected(val serverId: String) : ServerSelectState()
    data class Error(val message: String) : ServerSelectState()
}

@HiltViewModel
class ServerSelectViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ServerSelectState>(ServerSelectState.Idle)
    val state: StateFlow<ServerSelectState> = _state.asStateFlow()

    val servers = serverRepository.serversFlow

    fun checkServerHealth(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.value = ServerSelectState.Loading
            when (val result = serverRepository.checkServerHealth(url)) {
                is Result.Success -> {
                    onResult(result.data)
                    _state.value = ServerSelectState.Idle
                }
                is Result.Error -> {
                    _state.value = ServerSelectState.Error(
                        result.exception.message ?: "Connection failed"
                    )
                    onResult(false)
                }
                else -> {}
            }
        }
    }

    fun addServer(name: String, url: String, onResult: (Server?) -> Unit) {
        viewModelScope.launch {
            _state.value = ServerSelectState.Loading
            when (val result = serverRepository.addServer(name, url)) {
                is Result.Success -> {
                    _state.value = ServerSelectState.Idle
                    onResult(result.data)
                }
                is Result.Error -> {
                    _state.value = ServerSelectState.Error(
                        result.exception.message ?: "Failed to add server"
                    )
                    onResult(null)
                }
                else -> {}
            }
        }
    }

    fun removeServer(serverId: String) {
        viewModelScope.launch {
            serverRepository.removeServer(serverId)
        }
    }

    fun selectServer(serverId: String) {
        viewModelScope.launch {
            serverRepository.updateLastUsed(serverId)
            // 选择服务器时立即更新 SERVER_URL_KEY，确保后续 API 请求使用正确的服务器地址
            val server = serverRepository.getServerById(serverId)
            if (server != null) {
                preferencesRepository.saveServerUrl(server.url)
                ImageUrlHelper.setBaseUrl(server.url)
            }
            _state.value = ServerSelectState.ServerSelected(serverId)
        }
    }

    fun clearError() {
        _state.value = ServerSelectState.Idle
    }
}
