package com.nastv.data.repository

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.nastv.utils.AdBlockUtils
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * 自定义 DataSource，包装原始 HTTP DataSource，
 * 在启用广告屏蔽时拦截 m3u8 响应内容并过滤广告分片。
 */
@UnstableApi
class AdBlockDataSource(
    private val baseDataSource: DataSource,
    private val isEnabled: () -> Boolean,
    private val adBlockStrings: () -> List<String>
) : DataSource {

    private val tag = "AdBlockDataSource"
    private var uri: Uri? = null
    private var isM3u8 = false
    private var filteredData: ByteArray? = null
    private var readPosition = 0L

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val urlStr = uri.toString()
        isM3u8 = urlStr.contains(".m3u8", ignoreCase = true) ||
                urlStr.contains("m3u8", ignoreCase = true)

        val result = baseDataSource.open(dataSpec)

        if (isM3u8 && isEnabled()) {
            Log.d(tag, "拦截 m3u8 内容: $urlStr")
            // 读取所有数据
            val buffer = ByteArrayOutputStream()
            val tempBuf = ByteArray(4096)
            var bytesRead: Int
            while (baseDataSource.read(tempBuf, 0, tempBuf.size).also { bytesRead = it } != C.RESULT_END_OF_INPUT) {
                buffer.write(tempBuf, 0, bytesRead)
            }
            val content = buffer.toString(Charset.forName("UTF-8").name())

            Log.d(tag, "原始 m3u8 长度: ${content.length}")

            val result = AdBlockUtils.filterAdSegments(content, urlStr, adBlockStrings())
            Log.d(tag, "过滤后 m3u8 长度: ${result.filtered.length}, 广告数: ${result.adCount}")

            if (result.adCount > 0) {
                Log.d(tag, "过滤了 $result.adCount 个广告分片")
            }

            filteredData = result.filtered.toByteArray(Charset.forName("UTF-8"))
            readPosition = 0L
            return filteredData!!.size.toLong()
        }

        filteredData = null
        return result
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (filteredData != null) {
            val remaining = filteredData!!.size - readPosition
            if (remaining <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
            val bytesToRead = minOf(length.toLong(), remaining).toInt()
            System.arraycopy(filteredData!!, readPosition.toInt(), buffer, offset, bytesToRead)
            readPosition += bytesToRead
            return bytesToRead
        }
        return baseDataSource.read(buffer, offset, length)
    }

    override fun getUri(): Uri? {
        return filteredData?.let { uri } ?: baseDataSource.uri
    }

    override fun close() {
        filteredData = null
        readPosition = 0L
        baseDataSource.close()
    }

    override fun addTransferListener(transferListener: TransferListener) {
        baseDataSource.addTransferListener(transferListener)
    }
}

/**
 * DataSource.Factory 包装类，用于创建 AdBlockDataSource。
 */
@UnstableApi
class AdBlockDataSourceFactory(
    private val baseFactory: DataSource.Factory,
    private val isEnabled: () -> Boolean,
    private val adBlockStrings: () -> List<String>
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return AdBlockDataSource(
            baseDataSource = baseFactory.createDataSource(),
            isEnabled = isEnabled,
            adBlockStrings = adBlockStrings
        )
    }
}