package com.nastv.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Episode
import com.nastv.data.model.VideoDetail
import com.nastv.data.repository.PlayRepository
import com.nastv.data.repository.Result
import com.nastv.data.repository.SourceRepository
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
    private val preferencesRepository: PreferencesRepository,
    private val sourceRepository: SourceRepository
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

    private val _bufferMaxDuration = MutableStateFlow(100)
    val bufferMaxDuration: StateFlow<Int> = _bufferMaxDuration.asStateFlow()

    private val _bufferMinDuration = MutableStateFlow(50)
    val bufferMinDuration: StateFlow<Int> = _bufferMinDuration.asStateFlow()

    private val _cachedEpisodeIndices = MutableStateFlow<Set<Int>>(emptySet())
    val cachedEpisodeIndices: StateFlow<Set<Int>> = _cachedEpisodeIndices.asStateFlow()

    private val _sourceName = MutableStateFlow<String?>(null)
    val sourceName: StateFlow<String?> = _sourceName.asStateFlow()

    private val _adBlockEnabled = MutableStateFlow(false)
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _adBlockStrings = MutableStateFlow<List<String>>(emptyList())
    val adBlockStrings: StateFlow<List<String>> = _adBlockStrings.asStateFlow()

    init {
        viewModelScope.launch {
            _seekDuration.value = preferencesRepository.seekDurationFlow.first()
            _bufferMaxDuration.value = preferencesRepository.bufferMaxDurationFlow.first()
            _bufferMinDuration.value = preferencesRepository.bufferMinDurationFlow.first()
            _adBlockEnabled.value = preferencesRepository.adBlockEnabledFlow.first()
        }
        loadAdBlockStrings()
    }

    fun isAdBlockEnabled(): Boolean = _adBlockEnabled.value

    fun getAdBlockStrings(): List<String> = _adBlockStrings.value

    fun loadAdBlockStrings() {
        viewModelScope.launch {
            when (val result = playRepository.getAdBlockStrings()) {
                is Result.Success -> {
                    _adBlockStrings.value = result.data
                }
                is Result.Error -> {
                    android.util.Log.e("PlayViewModel", "获取广告屏蔽字符串失败", result.exception)
                }
                else -> {}
            }
        }
    }

    fun loadVideoDetail(sourceId: String, videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _restorePosition.value = null

            // 加载源名称
            when (val sourcesResult = sourceRepository.getSources()) {
                is Result.Success -> {
                    _sourceName.value = sourcesResult.data.find { it.id == sourceId }?.name
                }
                else -> {
                    _sourceName.value = null
                }
            }

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
                            // 异步加载缓存状态后尝试缓存播放
                            loadCacheStatus(sourceId, videoId, onReady = {
                                playEpisode(restoredEpisode)
                            })
                        } else {
                            loadCacheStatus(sourceId, videoId)
                        }
                    } else {
                        loadCacheStatus(sourceId, videoId)
                    }

                    // Save the progress position to restore after player is ready
                    if (savedProgress != null && savedProgress > 0) {
                        _restorePosition.value = (savedProgress * 1000).toLong() // Convert seconds to ms
                    }

                    checkFavoriteStatus(sourceId, videoId)
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

    private fun loadCacheStatus(sourceId: String, videoId: String, onReady: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = playRepository.getCacheStatus(sourceId, videoId)
            if (result is Result.Success) {
                val cachedIndices = result.data.episodes
                    .filter { it.value.state == "completed" }
                    .mapNotNull { it.key.toIntOrNull() }
                    .toSet()
                _cachedEpisodeIndices.value = cachedIndices
            }
            onReady?.invoke()
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
        val oldLine = _selectedLine.value
        _selectedLine.value = lineIndex
        val detail = _videoDetail.value ?: return
        if (lineIndex >= detail.episodes.size) return

        val newLineEpisodes = detail.episodes[lineIndex]
        if (newLineEpisodes.isEmpty()) return

        // 按位置匹配：用当前剧集在旧线路中的位置选新线路对应位置的剧集
        val oldLineEpisodes = detail.episodes.getOrNull(oldLine) ?: emptyList()
        val currentIdx = oldLineEpisodes.indexOfFirst { it.url == _currentEpisodeUrl.value }
        val targetIdx = if (currentIdx >= 0) currentIdx.coerceIn(0, newLineEpisodes.size - 1) else 0
        val episode = newLineEpisodes[targetIdx]

        _currentEpisodeUrl.value = episode.url
        _currentEpisodeName.value = episode.name
        playEpisode(episode)
    }

    fun playEpisode(episode: Episode) {
        _currentEpisodeName.value = episode.name
        _currentEpisodeUrl.value = episode.url
        viewModelScope.launch {
            val detail = _videoDetail.value
            val episodes = detail?.episodes?.getOrNull(_selectedLine.value) ?: emptyList()
            val episodeIndex = episodes.indexOfFirst { it.url == episode.url }

            android.util.Log.d("PlayViewModel", "playEpisode: idx=$episodeIndex, cached=${_cachedEpisodeIndices.value}, url=${episode.url}")

            // 已缓存则使用本地缓存播放
            if (episodeIndex >= 0 && episodeIndex in _cachedEpisodeIndices.value) {
                val baseUrl = preferencesRepository.getServerUrl()
                val cacheUrl = "$baseUrl/api/cache/${detail!!.sourceId}/${detail.id}/$episodeIndex.m3u8"
                android.util.Log.d("PlayViewModel", "Using cache: $cacheUrl")
                _proxyUrl.value = cacheUrl
                return@launch
            }

            android.util.Log.d("PlayViewModel", "Using resolve API for: ${episode.url}")
            // 未缓存则通过 resolve 接口获取播放地址
            when (val result = playRepository.resolvePlayUrl(episode.url)) {
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

    fun updateBufferSettings(minDuration: Int, maxDuration: Int) {
        _bufferMinDuration.value = minDuration
        _bufferMaxDuration.value = maxDuration
        viewModelScope.launch {
            preferencesRepository.saveBufferMinDuration(minDuration)
            preferencesRepository.saveBufferMaxDuration(maxDuration)
        }
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
