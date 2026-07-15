package com.nastv.utils

import android.util.Log
import java.net.URL

/**
 * 分析 m3u8 内容，识别并过滤广告分片
 * 检测规则：
 * 1. 分片链接的域名与其他分片明显不同
 * 2. 分片链接的路径长度与其他分片明显不同
 * 3. 分片链接包含广告屏蔽字符串列表中的关键词
 */
object AdBlockUtils {

    private const val TAG = "AdBlock"

    data class FilterResult(
        val filtered: String,
        val adCount: Int
    )

    fun filterAdSegments(
        m3u8Content: String,
        baseUrl: String,
        blockStrings: List<String> = emptyList()
    ): FilterResult {
        val lines = m3u8Content.split("\n")
        val segmentUrls = mutableListOf<SegmentUrl>()
        var currentDuration = 0.0

        // 提取所有分片 URL
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val match = Regex("#EXTINF:([\\d.]+)").find(line)
                currentDuration = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                // 这是一个分片 URL
                val fullUrl = if (!line.startsWith("http")) {
                    // 相对路径，转换为绝对路径
                    val base = baseUrl.substringBeforeLast("/") + "/"
                    try {
                        URL(URL(base), line).toExternalForm()
                    } catch (e: Exception) {
                        line
                    }
                } else {
                    line
                }
                segmentUrls.add(SegmentUrl(url = fullUrl, lineIndex = i, duration = currentDuration))
            }
        }

        if (segmentUrls.size < 3) {
            return FilterResult(filtered = m3u8Content, adCount = 0)
        }

        // 分析域名分布
        val domainMap = mutableMapOf<String, Int>()
        for (seg in segmentUrls) {
            try {
                val domain = URL(seg.url).host
                domainMap[domain] = (domainMap[domain] ?: 0) + 1
            } catch (_: Exception) {
            }
        }

        // 找到主域名（出现次数最多的）
        var mainDomain = ""
        var maxCount = 0
        for ((domain, count) in domainMap) {
            if (count > maxCount) {
                maxCount = count
                mainDomain = domain
            }
        }

        // 分析路径长度分布
        val pathLengths = segmentUrls
            .filter { seg ->
                try {
                    URL(seg.url).host == mainDomain
                } catch (_: Exception) {
                    false
                }
            }
            .map { seg ->
                try {
                    URL(seg.url).path.length
                } catch (_: Exception) {
                    0
                }
            }

        // 计算主域名分片路径长度的中位数
        val sortedLengths = pathLengths.sorted()
        val medianLen = if (sortedLengths.isNotEmpty()) {
            sortedLengths[sortedLengths.size / 2]
        } else {
            0
        }

        // 识别广告分片
        val adLineIndexes = mutableSetOf<Int>()
        for (seg in segmentUrls) {
            try {
                val urlObj = URL(seg.url)
                val isDifferentDomain = urlObj.host != mainDomain
                val isAbnormalLength = if (medianLen > 0) {
                    kotlin.math.abs(urlObj.path.length - medianLen) > medianLen * 0.1
                } else false

                // 检查是否匹配广告屏蔽字符串
                var isBlockedString = false
                if (blockStrings.isNotEmpty()) {
                    val urlLower = seg.url.lowercase()
                    isBlockedString = blockStrings.any { str -> urlLower.contains(str.lowercase()) }
                    if (isBlockedString) {
                        Log.d(TAG, "[去广告 debug] 匹配到屏蔽字符串 url=${seg.url} blockStrings=$blockStrings")
                    }
                }

                Log.d(TAG, "[去广告 debug] 分片检测 url=${seg.url} isDifferentDomain=$isDifferentDomain isAbnormalLength=$isAbnormalLength isBlockedString=$isBlockedString mainDomain=$mainDomain medianLen=$medianLen pathLen=${urlObj.path.length}")

                if (isDifferentDomain || isAbnormalLength || isBlockedString) {
                    adLineIndexes.add(seg.lineIndex)
                }
            } catch (_: Exception) {
            }
        }

        if (adLineIndexes.isEmpty()) {
            return FilterResult(filtered = m3u8Content, adCount = 0)
        }

        // 过滤掉广告分片（移除 #EXTINF 行和 URL 行）
        val filteredLines = mutableListOf<String>()
        for (i in lines.indices) {
            val line = lines[i]
            if (adLineIndexes.contains(i)) {
                // 这是广告 URL 行，同时需要移除前面的 #EXTINF 行
                if (filteredLines.isNotEmpty() && filteredLines.last().startsWith("#EXTINF:")) {
                    filteredLines.removeAt(filteredLines.size - 1)
                }
                continue
            }
            // 检查 #EXT-X-DISCONTINUITY 是否在广告边界
            if (line.trim() == "#EXT-X-DISCONTINUITY") {
                // 如果前后有广告分片，跳过这个标记
                val prevIsAd = adLineIndexes.contains(i - 1)
                val nextIsAd = i + 1 < lines.size && adLineIndexes.contains(i + 1)
                if (prevIsAd || nextIsAd) {
                    continue
                }
            }
            filteredLines.add(line)
        }

        return FilterResult(
            filtered = filteredLines.joinToString("\n"),
            adCount = adLineIndexes.size
        )
    }

    private data class SegmentUrl(
        val url: String,
        val lineIndex: Int,
        val duration: Double
    )
}