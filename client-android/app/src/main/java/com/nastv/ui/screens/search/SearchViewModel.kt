package com.nastv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.Video
import com.nastv.data.model.VideoSource
import com.nastv.data.repository.SearchRepository
import com.nastv.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 手机端搜索：与 TV 端一致，按站源并发直连搜索，不再走后端聚合接口。
 * 联想词仍然走后端 /api/search/suggest。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _sourceNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val sourceNameMap: StateFlow<Map<String, String>> = _sourceNameMap.asStateFlow()

    /** 已启用的站源，顺序即搜索结果的分组顺序 */
    private var enabledSources: List<VideoSource> = emptyList()

    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 最近一次提交搜索的关键词，用于区分「还没搜」和「搜了没结果」 */
    private val _searchedKeyword = MutableStateFlow("")
    val searchedKeyword: StateFlow<String> = _searchedKeyword.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { applySources() }
    }

    private suspend fun applySources(): List<VideoSource> {
        sourceRepository.getSources().getOrNull()?.let { sources ->
            enabledSources = sources.filter { it.enabled }
            _sourceNameMap.value = sources.associate { it.id to it.name }
        }
        return enabledSources
    }

    /** 站源列表可能还没加载完就点了搜索，这里兜底再取一次 */
    private suspend fun resolveSources(): List<VideoSource> =
        if (enabledSources.isEmpty()) applySources() else enabledSources

    /**
     * 并发搜索所有已启用站源。
     * 每个源返回后立即追加到结果里（按站源顺序重新拼装，避免结果位置乱跳）。
     */
    fun search(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _suggestions.value = emptyList()
            _searchResults.value = emptyList()
            _searchedKeyword.value = trimmed

            val sources = resolveSources()
            if (sources.isEmpty()) {
                _isLoading.value = false
                _errorMessage.value = "没有可用的站源"
                return@launch
            }

            val order = sources.map { it.id }
            val perSource = mutableMapOf<String, List<Video>>()
            var failedCount = 0

            try {
                coroutineScope {
                    sources.forEach { source ->
                        launch {
                            val videos = sourceRepository
                                .getVideos(sourceId = source.id, keyword = trimmed)
                                .getOrNull()
                            if (videos.isNullOrEmpty()) {
                                failedCount++
                            } else {
                                perSource[source.id] = videos.map { it.copy(sourceId = source.id) }
                                _searchResults.value = order.flatMap { perSource[it].orEmpty() }
                            }
                        }
                    }
                }
            } finally {
                _isLoading.value = false
            }

            if (perSource.isEmpty() && failedCount > 0) {
                _errorMessage.value = "搜索失败：${failedCount} 个站源都没有返回结果，请检查网络或站点状态"
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
            searchRepository.getSuggestions(keyword).getOrNull()?.let { words ->
                _suggestions.value = words
            }
        }
    }
}
