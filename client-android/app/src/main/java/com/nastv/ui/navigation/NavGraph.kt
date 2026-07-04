package com.nastv.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.nastv.ui.screens.settings.SettingsScreen
import com.nastv.ui.theme.LocalIsTv

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesRepository: PreferencesRepository = hiltViewModel<PreferencesViewModel>().repository
) {
    val isLoggedIn by preferencesRepository.authTokenFlow.collectAsState(initial = null)

    val startDestination = if (isLoggedIn.isNullOrEmpty()) Screen.Login.route else Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Login.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
