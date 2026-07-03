package com.nastv.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.repository.AuthRepository
import com.nastv.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val serverUrl: Flow<String> = preferencesRepository.serverUrlFlow

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            // Normalize and save server URL
            val normalizedUrl = if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
                serverUrl.removeSuffix("/")
            } else {
                "http://${serverUrl.removeSuffix("/")}"
            }
            preferencesRepository.saveServerUrl(normalizedUrl)

            when (val result = authRepository.login(username, password)) {
                is Result.Success -> {
                    val user = result.data
                    preferencesRepository.saveAuthToken(user.token)
                    preferencesRepository.saveUsername(user.username)
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
}
