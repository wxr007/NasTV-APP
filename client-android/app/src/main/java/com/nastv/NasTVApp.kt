package com.nastv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.nastv.data.local.PreferencesRepository
import com.nastv.utils.Constants
import com.nastv.utils.ImageUrlHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NasTVApp : Application(), ImageLoaderFactory {

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
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .logger(DebugLogger())
            .build()
    }
}
