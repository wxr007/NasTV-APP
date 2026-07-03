package com.nastv.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Episode
import com.nastv.data.model.VideoDetail
import com.nastv.data.repository.PlayRepository
import com.nastv.data.repository.Result
import com.nastv.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val playRepository: PlayRepository,
    private val userDataRepository: UserDataRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _videoDetail = MutableStateFlow<VideoDetail?>(null)
    val videoDetail: StateFlow<VideoDetail?> = _videoDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedLine = MutableStateFlow(0)
    val selectedLine: StateFlow<Int> = _selectedLine.asStateFlow()

    private val _currentEpisodeUrl = MutableStateFlow<String?>(null)
    val currentEpisodeUrl: StateFlow<String?> = _currentEpisodeUrl.asStateFlow()

    private val _currentEpisodeName = MutableStateFlow<String?>(null)
    val currentEpisodeName: StateFlow<String?> = _currentEpisodeName.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _proxyUrl = MutableStateFlow<String?>(null)
    val proxyUrl: StateFlow<String?> = _proxyUrl.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _restorePosition = MutableStateFlow<Long?>(null)
    val restorePosition: StateFlow<Long?> = _restorePosition.asStateFlow()

    private val _seekDuration = MutableStateFlow(10)
    val seekDuration: StateFlow<Int> = _seekDuration.asStateFlow()

    private val _cachedEpisodeIndices = MutableStateFlow<Set<Int>>(emptySet())
    val cachedEpisodeIndices: StateFlow<Set<Int>> = _cachedEpisodeIndices.asStateFlow()

    init {
        viewModelScope.launch {
            _seekDuration.value = preferencesRepository.seekDurationFlow.first()
        }
    }

    fun loadVideoDetail(sourceId: String, videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _restorePosition.value = null

            // First, check if there's play progress to restore
            val progressResult = userDataRepository.getPlayProgress(sourceId, videoId)
            val savedProgress: Float? = if (progressResult is Result.Success) {
                progressResult.data.progress
            } else null
            val savedLine = if (progressResult is Result.Success) progressResult.data.activeSource ?: 0 else 0
            val savedEpisodeIndex = if (progressResult is Result.Success) progressResult.data.activeEpisodeIndex ?: 0 else 0

            when (val result = playRepository.getVideoDetail(sourceId, videoId)) {
                is Result.Success -> {
                    _videoDetail.value = result.data
                    _isLoading.value = false

                    val episodes = result.data.episodes

                    // Restore line selection from history
                    if (episodes.isNotEmpty()) {
                        val restoredLine = savedLine.coerceIn(0, episodes.size - 1)
                        _selectedLine.value = restoredLine

                        val lineEpisodes = episodes[restoredLine]
                        if (lineEpisodes.isNotEmpty()) {
                            // Restore episode selection from history
                            val restoredEpisodeIndex = savedEpisodeIndex.coerceIn(0, lineEpisodes.size - 1)
                            val restoredEpisode = lineEpisodes[restoredEpisodeIndex]
                            _currentEpisodeUrl.value = restoredEpisode.url
                            _currentEpisodeName.value = restoredEpisode.name
                        }
                    }

                    // Save the progress position to restore after player is ready
                    if (savedProgress != null && savedProgress > 0) {
                        _restorePosition.value = (savedProgress * 1000).toLong() // Convert seconds to ms
                    }

                    checkFavoriteStatus(sourceId, videoId)
                    loadCacheStatus(sourceId, videoId)
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.message
                    _isLoading.value = false
                }
                else -> {}
            }
        }
    }

    private fun checkFavoriteStatus(sourceId: String, videoId: String) {
        viewModelScope.launch {
            val result = userDataRepository.getFavorites()
            if (result is Result.Success) {
                val key = "$sourceId:$videoId"
                _isFavorite.value = result.data.containsKey(key)
            }
        }
    }

    private fun loadCacheStatus(sourceId: String, videoId: String) {
        viewModelScope.launch {
            val result = playRepository.getCacheStatus(sourceId, videoId)
            if (result is Result.Success) {
                val cachedIndices = result.data.episodes
                    .filter { it.value.state == "completed" }
                    .mapNotNull { it.key.toIntOrNull() }
                    .toSet()
                _cachedEpisodeIndices.value = cachedIndices
            }
        }
    }

    fun toggleFavorite() {
        val detail = _videoDetail.value ?: return
        viewModelScope.launch {
            if (_isFavorite.value) {
                val result = userDataRepository.removeFavorite(detail.sourceId, detail.id)
                if (result is Result.Success) {
                    _isFavorite.value = false
                } else if (result is Result.Error) {
                    _errorMessage.value = "取消收藏失败: ${result.exception.message}"
                }
            } else {
                val result = userDataRepository.addFavorite(
                    sourceId = detail.sourceId,
                    videoId = detail.id,
                    name = detail.name,
                    pic = detail.pic,
                    remark = detail.type
                )
                if (result is Result.Success) {
                    _isFavorite.value = true
                } else if (result is Result.Error) {
                    _errorMessage.value = "收藏失败: ${result.exception.message}"
                }
            }
        }
    }

    fun selectLine(lineIndex: Int) {
        _selectedLine.value = lineIndex
        val detail = _videoDetail.value
        if (detail != null && lineIndex < detail.episodes.size && detail.episodes[lineIndex].isNotEmpty()) {
            val firstEpisode = detail.episodes[lineIndex][0]
            _currentEpisodeUrl.value = firstEpisode.url
            _currentEpisodeName.value = firstEpisode.name
        }
    }

    fun playEpisode(episode: Episode) {
        _currentEpisodeName.value = episode.name
        viewModelScope.launch {
            when (val result = playRepository.proxyM3u8(episode.url)) {
                is Result.Success -> {
                    _proxyUrl.value = result.data
                }
                is Result.Error -> {
                    _proxyUrl.value = null
                    _currentEpisodeUrl.value = episode.url
                }
                else -> {}
            }
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun recordPlayHistory(
        sourceId: String,
        videoId: String,
        episode: String,
        videoName: String,
        pic: String? = null,
        progress: Float = 0f,
        duration: Float? = null,
        totalEpisodes: Int? = null,
        activeSource: Int? = null,
        activeEpisodeIndex: Int? = null
    ) {
        viewModelScope.launch {
            userDataRepository.updatePlayProgress(
                sourceId = sourceId,
                videoId = videoId,
                progress = progress,
                episode = episode,
                name = videoName,
                pic = pic,
                duration = duration,
                totalEpisodes = totalEpisodes,
                activeSource = activeSource,
                activeEpisodeIndex = activeEpisodeIndex
            )
        }
    }
}
