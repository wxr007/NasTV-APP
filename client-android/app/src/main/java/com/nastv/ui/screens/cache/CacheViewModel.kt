package com.nastv.ui.screens.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.CachedEpisode
import com.nastv.data.repository.Result
import com.nastv.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CacheViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    private val _episodes = MutableStateFlow<List<CachedEpisode>>(emptyList())
    val episodes: StateFlow<List<CachedEpisode>> = _episodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadCachedEpisodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = userDataRepository.getCachedEpisodes()) {
                is Result.Success -> {
                    _episodes.value = result.data
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message
                    _isLoading.value = false
                }
                else -> {}
            }
        }
    }
}
