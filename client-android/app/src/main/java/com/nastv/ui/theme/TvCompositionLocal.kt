package com.nastv.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal to provide TV mode status throughout the app.
 * Use `LocalIsTv.current` to check if the device is a TV.
 */
val LocalIsTv = compositionLocalOf { false }
