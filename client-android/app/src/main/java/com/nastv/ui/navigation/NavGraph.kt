package com.nastv.ui.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nastv.data.local.PreferencesRepository
import com.nastv.ui.screens.cache.CacheScreen
import com.nastv.ui.screens.favorites.FavoritesScreen
import com.nastv.ui.screens.history.HistoryScreen
import com.nastv.ui.screens.home.HomeScreen
import com.nastv.ui.screens.home.TVHomeScreen
import com.nastv.ui.screens.login.LoginScreen
import com.nastv.ui.screens.play.PlayScreen
import com.nastv.ui.screens.search.SearchScreen
import com.nastv.ui.screens.server.ServerSelectScreen
import com.nastv.ui.screens.settings.SettingsScreen
import com.nastv.ui.theme.LocalIsTv

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesRepository: PreferencesRepository = hiltViewModel<PreferencesViewModel>().repository
) {
    var initialNavigationDone by remember { mutableStateOf(false) }
    
    Log.d("NavGraph", "NavGraph recomposed, initialNavigationDone=$initialNavigationDone")
    
    // 初始导航：从 DataStore 读取当前状态后导航到正确的页面
    LaunchedEffect(Unit) {
        val token = preferencesRepository.authTokenFlow.first()
        val serverId = preferencesRepository.lastServerIdFlow.first()
        Log.d("NavGraph", "Initial navigation check: token=$token, lastServerId=$serverId")
        
        val destination = when {
            !token.isNullOrEmpty() -> Screen.Home.route
            !serverId.isNullOrEmpty() -> Screen.Login.createRoute(serverId)
            else -> null
        }
        
        if (destination != null) {
            Log.d("NavGraph", "Initial navigation to: $destination")
            navController.navigate(destination) {
                popUpTo(Screen.ServerSelect.route) { inclusive = true }
            }
        }
        initialNavigationDone = true
    }

    NavHost(
        navController = navController,
        startDestination = Screen.ServerSelect.route
    ) {
        composable(
            route = Screen.ServerSelect.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            ServerSelectScreen(
                onServerSelected = { serverId ->
                    navController.navigate(Screen.Login.createRoute(serverId))
                }
            )
        }
        
        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            Log.d("NavGraph", "Login composable entered, serverId=$serverId")
            LoginScreen(
                serverId = serverId,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(Screen.ServerSelect.route) {
                        popUpTo(Screen.ServerSelect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Home.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val isTv = LocalIsTv.current
            if (isTv) {
                TVHomeScreen(
                    onNavigateToPlay = { sourceId, videoId ->
                        navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.Favorites.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCache = {
                        navController.navigate(Screen.Cache.route)
                    }
                )
            } else {
                HomeScreen(
                    onNavigateToPlay = { sourceId, videoId ->
                        navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.Favorites.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCache = {
                        navController.navigate(Screen.Cache.route)
                    }
                )
            }
        }

        composable(
            route = Screen.Search.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SearchScreen(
                onNavigateToPlay = { sourceId, videoId ->
                    navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Play.route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("videoId") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            PlayScreen(
                sourceId = sourceId,
                videoId = videoId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Favorites.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            FavoritesScreen(
                onNavigateToPlay = { sourceId, videoId ->
                    navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.History.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            HistoryScreen(
                onNavigateToPlay = { sourceId, videoId ->
                    navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val scope = rememberCoroutineScope()
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    // 退出登录后，从 DataStore 读取 lastServerId，导航到登录页面
                    scope.launch {
                        val serverId = preferencesRepository.lastServerIdFlow.first()
                        Log.d("NavGraph", "onLogout called, lastServerId=$serverId")
                        if (!serverId.isNullOrEmpty()) {
                            // 导航到登录页，弹出到服务器选择页
                            val route = Screen.Login.createRoute(serverId)
                            Log.d("NavGraph", "Navigating to login with route: $route")
                            navController.navigate(route) {
                                popUpTo(Screen.ServerSelect.route) { inclusive = true }
                            }
                        } else {
                            // 如果没有 lastServerId，导航到服务器选择页面
                            Log.d("NavGraph", "Navigating to server select")
                            navController.navigate(Screen.ServerSelect.route) {
                                popUpTo(Screen.ServerSelect.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.Cache.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CacheScreen(
                onNavigateToPlay = { sourceId, videoId ->
                    navController.navigate(Screen.Play.createRoute(sourceId, videoId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * ViewModel to expose PreferencesRepository to the NavGraph.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class PreferencesViewModel @javax.inject.Inject constructor(
    val repository: PreferencesRepository
) : androidx.lifecycle.ViewModel()
