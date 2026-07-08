package com.nastv.utils

object Constants {
    // DataStore
    const val DATASTORE_NAME = "nastv_preferences"

    // Preference keys
    const val PREF_SERVER_URL = "server_url"
    const val PREF_AUTH_TOKEN = "auth_token"
    const val PREF_USERNAME = "username"
    const val PREF_USE_IMAGE_PROXY = "use_image_proxy"
    const val PREF_SEEK_DURATION = "seek_duration"
    const val PREF_FORCE_TV_MODE = "force_tv_mode"
    const val PREF_THEME_MODE = "theme_mode" // "system", "dark", "light"
    const val PREF_USER_ROLE = "user_role"

    // Default server URL (placeholder, replaced by interceptor at runtime)
    const val DEFAULT_BASE_URL = "http://placeholder/"

    // Network timeouts (seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val DEFAULT_PAGE = 1

    // Search debounce (milliseconds)
    const val SEARCH_DEBOUNCE_MS = 300L
}
