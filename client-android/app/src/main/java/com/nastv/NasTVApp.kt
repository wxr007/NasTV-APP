package com.nastv

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.nastv.data.local.PreferencesRepository
import com.nastv.utils.Constants
import com.nastv.utils.ImageUrlHelper
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NasTVApp : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "NasTVApp"
    }

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        // Initialize ImageUrlHelper early so image URLs are correct at first composition
        val baseUrl = runBlocking {
            preferencesRepository.serverUrlFlow.first()
        }
        ImageUrlHelper.setBaseUrl(baseUrl)

        val useImageProxy = runBlocking {
            preferencesRepository.useImageProxyFlow.first()
        }
        ImageUrlHelper.setUseImageProxy(useImageProxy)
    }

    override fun newImageLoader(): ImageLoader {
        // AVIF 转换拦截器：CDN 有时返回 .jpg 后缀但实际是 AVIF 格式，
        // 在 HTTP 层用 avif-coder (libheif) 解码后转为 JPEG
        val avifInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val body = response.body ?: return@Interceptor response

            val bytes = try {
                body.bytes()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read image body", e)
                return@Interceptor response
            }

            // 检查 AVIF 文件头 (ftyp box = "avif")
            val isAvif = bytes.size >= 12 &&
                bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() &&
                bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte() &&
                bytes[8] == 0x61.toByte() && bytes[9] == 0x76.toByte() &&
                bytes[10] == 0x69.toByte() && bytes[11] == 0x66.toByte()

            if (!isAvif) {
                return@Interceptor response.newBuilder()
                    .body(bytes.toResponseBody(body.contentType()))
                    .build()
            }

            try {
                val decoded = HeifCoder().decode(bytes)
                val jpegStream = ByteArrayOutputStream()
                decoded.compress(Bitmap.CompressFormat.JPEG, 90, jpegStream)
                decoded.recycle()
                val jpegBytes = jpegStream.toByteArray()

                return@Interceptor response.newBuilder()
                    .body(jpegBytes.toResponseBody("image/jpeg".toMediaType()))
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "AVIF decode failed", e)
            }

            response.newBuilder()
                .body(bytes.toResponseBody(body.contentType()))
                .build()
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(avifInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .callFactory { okHttpClient }
            .logger(DebugLogger())
            .build()
    }
}
