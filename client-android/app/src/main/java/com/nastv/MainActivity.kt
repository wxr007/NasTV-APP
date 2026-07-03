package com.nastv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.nastv.data.local.PreferencesRepository
import com.nastv.ui.navigation.NavGraph
import com.nastv.ui.theme.LocalIsTv
import com.nastv.ui.theme.NasTVTheme
import com.nastv.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val forceTvMode by preferencesRepository.forceTvModeFlow.collectAsState(initial = false)
            val isTv = remember(forceTvMode) {
                if (forceTvMode) true else DeviceUtils.isTvDevice(context)
            }

            NasTVTheme {
                CompositionLocalProvider(LocalIsTv provides isTv) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

/**
 * Detect if the current device has a touchscreen.
 * Returns true for phones/tablets, false for TV devices.
 */
@Composable
fun isTouchDevice(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.touchscreen != 0
}
