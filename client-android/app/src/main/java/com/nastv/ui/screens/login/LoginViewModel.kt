package com.nastv.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Account
import com.nastv.data.model.Server
import com.nastv.data.repository.AccountRepository
import com.nastv.data.repository.Result
import com.nastv.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val serverRepository: ServerRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _server = MutableStateFlow<Server?>(null)
    val server: StateFlow<Server?> = _server.asStateFlow()

    private val _serverId = MutableStateFlow<String?>(null)
    
    val accounts: Flow<List<Account>> = _serverId.flatMapLatest { serverId ->
        Log.d("LoginViewModel", "flatMapLatest triggered with serverId=$serverId")
        if (serverId != null) {
            accountRepository.getAccountsForServer(serverId)
        } else {
            flowOf(emptyList())
        }
    }

    fun loadServer(serverId: String) {
        Log.d("LoginViewModel", "loadServer called with serverId=$serverId")
        _serverId.value = serverId
        viewModelScope.launch {
            val serverData = serverRepository.getServerById(serverId)
            Log.d("LoginViewModel", "serverData loaded: $serverData")
            _server.value = serverData
        }
    }

    fun loginWithAccount(account: Account) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            // 先保存服务器URL到PreferencesRepository，供API调用使用
            val serverData = _server.value
            if (serverData != null) {
                preferencesRepository.saveServerUrl(serverData.url)
            }

            when (val result = accountRepository.loginWithAccount(account)) {
                is Result.Success -> {
                    _loginState.value = LoginState.Success
                }
                is Result.Error -> {
                    _loginState.value = LoginState.Error(
                        result.exception.message ?: "Login failed"
                    )
                }
                else -> {}
            }
        }
    }

    fun addAccount(username: String, password: String, onResult: (Boolean) -> Unit) {
        val serverId = _serverId.value ?: return
        viewModelScope.launch {
            when (accountRepository.addAccount(serverId, username, password)) {
                is Result.Success -> onResult(true)
                is Result.Error -> onResult(false)
                else -> {}
            }
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.removeAccount(accountId)
        }
    }
}
