package com.nastv.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.model.FavoriteItem
import com.nastv.data.repository.Result
import com.nastv.data.repository.SourceRepository
import com.nastv.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Favorite entry with parsed sourceId and videoId from the key.
 */
data class FavoriteEntry(
    val key: String,
    val sourceId: String,
    val videoId: String,
    val item: FavoriteItem
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
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

    private val _favorites = MutableStateFlow<List<FavoriteEntry>>(emptyList())
    val favorites: StateFlow<List<FavoriteEntry>> = _favorites.asStateFlow()

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

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = userDataRepository.getFavorites()) {
                is Result.Success -> {
                    val entries = result.data.map { (key, item) ->
                        val parts = key.split(":")
                        FavoriteEntry(
                            key = key,
                            sourceId = parts.getOrElse(0) { "" },
                            videoId = parts.getOrElse(1) { "" },
                            item = item
                        )
                    }
                    _favorites.value = entries
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

    fun removeFavorite(sourceId: String, videoId: String) {
        viewModelScope.launch {
            when (val result = userDataRepository.removeFavorite(sourceId, videoId)) {
                is Result.Success -> {
                    _favorites.value = _favorites.value.filter {
                        it.sourceId != sourceId || it.videoId != videoId
                    }
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }
}
