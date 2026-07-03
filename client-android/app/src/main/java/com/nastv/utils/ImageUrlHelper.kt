package com.nastv.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Utility class to provide the current server base URL for image loading.
 * This is used by Coil to construct full URLs for image proxy requests.
 */
object ImageUrlHelper {
    private var _baseUrl: String? = null
    private var _useImageProxy: Boolean = true

    fun setBaseUrl(url: String) {
        _baseUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url.trimEnd('/')
        } else {
            "http://$url".trimEnd('/')
        }
    }

    fun setUseImageProxy(useProxy: Boolean) {
        _useImageProxy = useProxy
    }

    fun isUseImageProxy(): Boolean = _useImageProxy

    fun getBaseUrl(): String {
        return _baseUrl ?: "http://placeholder"
    }

    /**
     * Convert a relative image URL to a full proxy URL.
     * Example: /api/play/image?url=... -> http://192.168.1.4:3700/api/play/image?url=...
     */
    fun toFullUrl(relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        return "${getBaseUrl()}$relativeUrl"
    }
}
