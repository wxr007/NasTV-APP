package com.nastv.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nastv.data.local.PreferencesRepository
import com.nastv.data.model.Video
import com.nastv.data.model.VideoSource
import com.nastv.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class ApiProvider {
    TENCENT,      // 腾讯：热词=360kan + 联想=腾讯smartbox
    IQIYI_360,    // 爱奇艺+360kan：热词=360kan + 联想=爱奇艺suggest
    DOUBAN        // 豆瓣：热词=360kan + 联想=豆瓣suggest
}

@HiltViewModel
class TvSearchViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // === 输入页状态 ===
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _hotWords = MutableStateFlow<List<String>>(emptyList())
    val hotWords: StateFlow<List<String>> = _hotWords.asStateFlow()

    private val _suggestionWords = MutableStateFlow<List<String>>(emptyList())
    val suggestionWords: StateFlow<List<String>> = _suggestionWords.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _apiProvider = MutableStateFlow(ApiProvider.IQIYI_360)
    val apiProvider: StateFlow<ApiProvider> = _apiProvider.asStateFlow()

    // === 站源选择 ===
    private val _allSources = MutableStateFlow<List<VideoSource>>(emptyList())
    val allSources: StateFlow<List<VideoSource>> = _allSources.asStateFlow()

    private val _selectedSourceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSourceIds: StateFlow<Set<String>> = _selectedSourceIds.asStateFlow()

    // === 结果页状态 ===
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _sourceResults = MutableStateFlow<Map<String, List<Video>>>(emptyMap())
    val sourceResults: StateFlow<Map<String, List<Video>>> = _sourceResults.asStateFlow()

    private val _selectedSourceId = MutableStateFlow("all")
    val selectedSourceId: StateFlow<String> = _selectedSourceId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 防抖
    private var suggestionJob: Job? = null
    private var hotWordsLoaded = false

    private val externalClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // 确保 init 完成后再执行搜索
    private val initReady = CompletableDeferred<Unit>()

    init {
        Log.d("TvSearchVM", "init")
        viewModelScope.launch {
            // 先加载保存的偏好（必须等它完成）
            val savedProvider = preferencesRepository.searchApiProviderFlow.first()
            _apiProvider.value = when (savedProvider) {
                "tencent" -> ApiProvider.TENCENT
                "douban" -> ApiProvider.DOUBAN
                else -> ApiProvider.IQIYI_360
            }
            val savedIds = preferencesRepository.searchSelectedSourceIdsFlow.first()
            Log.d("TvSearchVM", "loaded saved sourceIds: $savedIds")
            if (savedIds.isNotEmpty()) {
                _selectedSourceIds.value = savedIds
            }
            val savedHistory = preferencesRepository.searchHistoryFlow.first()
            _searchHistory.value = savedHistory

            // 然后加载站源列表（此时已知道用户之前选了哪些）
            sourceRepository.getSources().getOrNull()?.let { sources ->
                _allSources.value = sources.filter { it.enabled }
                if (_selectedSourceIds.value.isEmpty()) {
                    _selectedSourceIds.value = sources.filter { it.enabled }.map { it.id }.toSet()
                    Log.d("TvSearchVM", "default all sources: ${_selectedSourceIds.value}")
                }
            }

            // 最后加载热词
            loadHotWords()
            initReady.complete(Unit)
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
        if (text.isBlank()) {
            _suggestionWords.value = emptyList()
        } else {
            loadSuggestions(text)
        }
    }

    fun appendInput(char: String) {
        _inputText.value = _inputText.value + char
        loadSuggestions(_inputText.value)
    }

    fun deleteLastChar() {
        val current = _inputText.value
        if (current.isNotEmpty()) {
            _inputText.value = current.dropLast(1)
            if (_inputText.value.isNotBlank()) {
                loadSuggestions(_inputText.value)
            } else {
                _suggestionWords.value = emptyList()
            }
        }
    }

    fun clearInput() {
        _inputText.value = ""
        _suggestionWords.value = emptyList()
    }

    // === 热词 ===
    fun loadHotWords() {
        viewModelScope.launch {
            try {
                // 腾讯视频热词 API 已下线，统一使用 360kan
                val url = "https://api.web.360kan.com/v1/rank?cat=1"
                Log.d("TvSearchVM", "loadHotWords: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://www.360kan.com/rank/general")
                    .build()
                externalClient.newCall(request).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    Log.d("TvSearchVM", "loadHotWords response: code=${resp.code}, bodyLen=${body.length}")
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val words = parseHotWords(body)
                        if (words.isNotEmpty()) {
                            _hotWords.value = words
                            hotWordsLoaded = true
                            Log.d("TvSearchVM", "loadHotWords success: ${words.size} words")
                        } else {
                            Log.d("TvSearchVM", "loadHotWords: parsed 0 words")
                        }
                    } else {
                        Log.d("TvSearchVM", "loadHotWords: not successful, code=${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TvSearchVM", "loadHotWords error: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun parseHotWords(json: String): List<String> {
        return try {
            val root = JSONObject(json)
            val dataArr = root.optJSONArray("data")
            if (dataArr != null) {
                val result = mutableListOf<String>()
                for (i in 0 until dataArr.length()) {
                    val item = dataArr.optJSONObject(i) ?: continue
                    val title = item.optString("title", "")
                    if (title.isNotBlank()) {
                        result.add(title)
                    } else {
                        val name = item.optString("name", "")
                        if (name.isNotBlank()) result.add(name)
                    }
                }
                result
            } else {
                val hotList = root.optJSONArray("hotlist")
                if (hotList != null) {
                    val result = mutableListOf<String>()
                    for (i in 0 until hotList.length()) {
                        val item = hotList.optJSONObject(i) ?: continue
                        val title = item.optString("title", "")
                        if (title.isNotBlank()) result.add(title)
                        else {
                            val name = item.optString("name", "")
                            if (name.isNotBlank()) result.add(name)
                        }
                    }
                    result
                } else emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // === 联想词（带 300ms 防抖） ===
    private fun loadSuggestions(keyword: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(300)
            try {
                val encoded = URLEncoder.encode(keyword, "UTF-8")
                val url = when (_apiProvider.value) {
                    ApiProvider.TENCENT ->
                        "https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box?format=json&page_num=0&page_size=20&key=$encoded"
                    ApiProvider.IQIYI_360 ->
                        "https://suggest.video.iqiyi.com/?if=mobile&key=$encoded"
                    ApiProvider.DOUBAN ->
                        "https://movie.douban.com/j/subject_suggest?q=$encoded"
                }
                Log.d("TvSearchVM", "loadSuggestions: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                externalClient.newCall(request).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    Log.d("TvSearchVM", "loadSuggestions response: code=${resp.code}, bodyLen=${body.length}")
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val words = parseSuggestions(body)
                        if (words.isNotEmpty()) {
                            _suggestionWords.value = words
                            Log.d("TvSearchVM", "loadSuggestions success: ${words.size} words")
                        } else {
                            Log.d("TvSearchVM", "loadSuggestions: parsed 0 words, bodyPreview=${body.take(500)}")
                        }
                    } else {
                        Log.d("TvSearchVM", "loadSuggestions: not successful, code=${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TvSearchVM", "loadSuggestions error: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun parseSuggestions(json: String): List<String> {
        // 先尝试数组格式（豆瓣: [{title: ...}, ...]）
        val arrayWords = parseArraySuggestions(json)
        if (arrayWords.isNotEmpty()) return arrayWords

        // 再尝试对象格式（腾讯、爱奇艺）
        return try {
            val root = JSONObject(json)
            val tencentWords = parseTencentSuggestions(root)
            if (tencentWords.isNotEmpty()) tencentWords
            else parseIqiyiSuggestions(root)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTencentSuggestions(root: JSONObject): List<String> {
        return try {
            val data = root.optJSONObject("data") ?: return emptyList()
            val searchData = data.optJSONObject("search_data") ?: return emptyList()
            val vecGroupData = searchData.optJSONArray("vecGroupData") ?: return emptyList()
            val result = mutableListOf<String>()
            for (gi in 0 until vecGroupData.length()) {
                val group = vecGroupData.optJSONObject(gi) ?: continue
                val groupData = group.optJSONArray("group_data") ?: continue
                for (di in 0 until groupData.length()) {
                    val item = groupData.optJSONObject(di) ?: continue
                    val action = item.optJSONObject("action") ?: continue
                    val actionArgs = action.optJSONObject("actionArgs") ?: continue
                    val keyword = actionArgs.optJSONObject("search_keyword") ?: continue
                    val strVal = keyword.optString("strVal", "")
                    if (strVal.isNotBlank()) result.add(strVal)
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseIqiyiSuggestions(root: JSONObject): List<String> {
        return try {
            val dataArr = root.optJSONArray("data") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val title = item.optString("title", "")
                if (title.isNotBlank()) result.add(title)
                else {
                    val name = item.optString("name", "")
                    if (name.isNotBlank()) result.add(name)
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseArraySuggestions(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i)
                if (item != null) {
                    // 豆瓣格式: title; 其他格式: name
                    val title = item.optString("title", "")
                    if (title.isNotBlank()) { result.add(title); continue }
                    val name = item.optString("name", "")
                    if (name.isNotBlank()) result.add(name)
                } else {
                    val s = arr.optString(i, "")
                    if (s.isNotBlank()) result.add(s)
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    // === API 提供商 ===
    fun setApiProvider(provider: ApiProvider) {
        _apiProvider.value = provider
        hotWordsLoaded = false
        loadHotWords()
        if (_inputText.value.isNotBlank()) {
            loadSuggestions(_inputText.value)
        }
        viewModelScope.launch {
            preferencesRepository.saveSearchApiProvider(
                when (provider) {
                    ApiProvider.TENCENT -> "tencent"
                    ApiProvider.DOUBAN -> "douban"
                    ApiProvider.IQIYI_360 -> "iqiyi_360"
                }
            )
        }
    }

    // === 站源 ===

    fun toggleSource(sourceId: String) {
        val current = _selectedSourceIds.value.toMutableSet()
        if (current.contains(sourceId)) {
            current.remove(sourceId)
        } else {
            current.add(sourceId)
        }
        _selectedSourceIds.value = current
    }

    fun setSelectedSourceIds(ids: Set<String>) {
        _selectedSourceIds.value = ids
        viewModelScope.launch {
            preferencesRepository.saveSearchSelectedSourceIds(ids)
        }
    }

    // === 搜索（直连站源） ===
    fun search(keyword: String) {
        Log.d("TvSearchVM", "search() called, keyword=$keyword, waiting for init...")
        _searchKeyword.value = keyword
        _selectedSourceId.value = "all"
        _sourceResults.value = emptyMap()
        _isSearching.value = true
        _isLoading.value = true

        viewModelScope.launch {
            initReady.await()
            val ids = _selectedSourceIds.value
            Log.d("TvSearchVM", "search() init done, keyword=$keyword, selectedSourceIds=$ids")
            if (ids.isEmpty()) {
                Log.w("TvSearchVM", "search() no sources selected!")
                _isSearching.value = false
                _isLoading.value = false
                return@launch
            }

            try {
                coroutineScope {
                    ids.forEach { sourceId ->
                        async {
                            try {
                                Log.d("TvSearchVM", "search() calling getVideos sourceId=$sourceId, keyword=$keyword")
                                val result = sourceRepository.getVideos(
                                    sourceId = sourceId,
                                    keyword = keyword
                                )
                                result.getOrNull()?.let { videos ->
                                    Log.d("TvSearchVM", "search() sourceId=$sourceId got ${videos.size} results")
                                    val current = _sourceResults.value.toMutableMap()
                                    current[sourceId] = videos.map { it.copy(sourceId = sourceId) }
                                    _sourceResults.value = current
                                } ?: run {
                                    Log.w("TvSearchVM", "search() sourceId=$sourceId returned null/failed: ${result.exceptionOrNull()?.message}")
                                }
                            } catch (e: Exception) {
                                Log.e("TvSearchVM", "search() sourceId=$sourceId error", e)
                            }
                        }
                    }
                }
            } finally {
                _isSearching.value = false
                _isLoading.value = false
                Log.d("TvSearchVM", "search() finished, total results map size=${_sourceResults.value.size}")
            }
        }
    }

    fun searchMore(page: Int) {
        val sourceId = _selectedSourceId.value
        if (sourceId == "all" || sourceId.isBlank()) return
        val keyword = _searchKeyword.value
        if (keyword.isBlank()) return

        viewModelScope.launch {
            try {
                val result = sourceRepository.getVideos(
                    sourceId = sourceId,
                    page = page,
                    keyword = keyword
                )
                result.getOrNull()?.let { newVideos ->
                    val current = _sourceResults.value.toMutableMap()
                    val existing = current[sourceId] ?: emptyList()
                    current[sourceId] = existing + newVideos.map { it.copy(sourceId = sourceId) }
                    _sourceResults.value = current
                }
            } catch (_: Exception) { }
        }
    }

    fun selectSource(sourceId: String) {
        _selectedSourceId.value = sourceId
    }

    // === 搜索历史 ===
    fun addToHistory(keyword: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(keyword)
        current.add(0, keyword)
        if (current.size > 15) {
            current.removeAt(current.size - 1)
        }
        _searchHistory.value = current
        viewModelScope.launch {
            preferencesRepository.saveSearchHistory(current)
        }
    }

    fun clearHistory() {
        _searchHistory.value = emptyList()
        viewModelScope.launch {
            preferencesRepository.saveSearchHistory(emptyList())
        }
    }
}
