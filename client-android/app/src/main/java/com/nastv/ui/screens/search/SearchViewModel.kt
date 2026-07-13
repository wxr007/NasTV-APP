package com.nastv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.Video
import com.nastv.data.repository.Result
import com.nastv.data.repository.SearchRepository
import com.nastv.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
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

    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var suggestionJob: Job? = null

    // 滚动位置保存（导航到播放页后恢复）
    var savedScrollIndex: Int = 0
        private set

    fun saveScrollState(index: Int) {
        savedScrollIndex = index
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _suggestions.value = emptyList()
            when (val result = searchRepository.search(keyword)) {
                is Result.Success -> {
                    _searchResults.value = result.data
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

    fun getSuggestions(keyword: String) {
        if (keyword.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(300) // Debounce
            when (val result = searchRepository.getSuggestions(keyword)) {
                is Result.Success -> {
                    _suggestions.value = result.data
                }
                is Result.Error -> {
                    // Silently fail for suggestions
                }
                else -> {}
            }
        }
    }
}
