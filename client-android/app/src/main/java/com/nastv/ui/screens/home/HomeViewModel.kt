package com.nastv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.Category
import com.nastv.data.model.Video
import com.nastv.data.model.VideoSource
import com.nastv.data.repository.Result
import com.nastv.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _sources = MutableStateFlow<List<VideoSource>>(emptyList())
    val sources: StateFlow<List<VideoSource>> = _sources.asStateFlow()

    private val _selectedSource = MutableStateFlow<VideoSource?>(null)
    val selectedSource: StateFlow<VideoSource?> = _selectedSource.asStateFlow()

    private val _parentCategories = MutableStateFlow<List<Category>>(emptyList())
    val parentCategories: StateFlow<List<Category>> = _parentCategories.asStateFlow()

    private val _subCategories = MutableStateFlow<List<Category>>(emptyList())
    val subCategories: StateFlow<List<Category>> = _subCategories.asStateFlow()

    private val _selectedParentCategory = MutableStateFlow<Category?>(null)
    val selectedParentCategory: StateFlow<Category?> = _selectedParentCategory.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<Category?>(null)
    val selectedSubCategory: StateFlow<Category?> = _selectedSubCategory.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage = 1

    fun loadSources() {
        viewModelScope.launch {
            when (val result = sourceRepository.getSources()) {
                is Result.Success -> {
                    _sources.value = result.data
                    if (_selectedSource.value == null && _sources.value.isNotEmpty()) {
                        _selectedSource.value = _sources.value.first()
                    }
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun selectSource(source: VideoSource) {
        _selectedSource.value = source
        _selectedParentCategory.value = null
        _selectedSubCategory.value = null
        _parentCategories.value = emptyList()
        _subCategories.value = emptyList()
        currentPage = 1
        _hasMore.value = true
    }

    fun selectParentCategory(category: Category?) {
        _selectedParentCategory.value = category
        _selectedSubCategory.value = null
        // Update sub-categories
        _subCategories.value = category?.children ?: emptyList()
        currentPage = 1
        _hasMore.value = true
    }

    fun selectSubCategory(category: Category?) {
        _selectedSubCategory.value = category
        currentPage = 1
        _hasMore.value = true
    }

    fun loadVideos(sourceId: String, categoryId: String? = null) {
        viewModelScope.launch {
            currentPage = 1
            _hasMore.value = true
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = sourceRepository.getVideos(sourceId, page = currentPage, category = categoryId)) {
                is Result.Success -> {
                    _videos.value = result.data
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

    fun loadMoreVideos() {
        if (_isLoadingMore.value || !_hasMore.value) return
        val sourceId = _selectedSource.value?.id ?: return
        val categoryId = _selectedSubCategory.value?.id ?: _selectedParentCategory.value?.id

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1
            when (val result = sourceRepository.getVideos(sourceId, page = nextPage, category = categoryId)) {
                is Result.Success -> {
                    val newVideos = result.data
                    if (newVideos.isNotEmpty()) {
                        _videos.value = _videos.value + newVideos
                        currentPage = nextPage
                    } else {
                        _hasMore.value = false
                    }
                    _isLoadingMore.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message
                    _isLoadingMore.value = false
                }
                else -> {}
            }
        }
    }

    fun loadCategories(sourceId: String) {
        viewModelScope.launch {
            when (val result = sourceRepository.getCategories(sourceId)) {
                is Result.Success -> {
                    _parentCategories.value = result.data
                }
                is Result.Error -> {
                    _parentCategories.value = emptyList()
                }
                else -> {}
            }
        }
    }
}
