package com.nastv.ui.screens.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.CachedVideo
import com.nastv.data.repository.Result
import com.nastv.data.repository.SourceRepository
import com.nastv.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CacheViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _sourceNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val sourceNameMap: StateFlow<Map<String, String>> = _sourceNameMap.asStateFlow()

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            when (val result = sourceRepository.getSources()) {
                is Result.Success -> {
                    _sourceNameMap.value = result.data.associate { it.id to it.name }
                }
                is Result.Error -> {
                    // Silently fail
                }
                else -> {}
            }
        }
    }

    private val _videos = MutableStateFlow<List<CachedVideo>>(emptyList())
    val videos: StateFlow<List<CachedVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 滚动位置保存（导航到播放页后恢复）
    var savedScrollIndex: Int = 0
        private set

    fun saveScrollState(index: Int) {
        savedScrollIndex = index
    }

    fun loadCachedVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = userDataRepository.getCachedVideos()) {
                is Result.Success -> {
                    _videos.value = result.data.sortedByDescending { it.updatedAt ?: "" }
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
