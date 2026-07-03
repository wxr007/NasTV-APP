package com.nastv.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Play : Screen("play/{sourceId}/{videoId}") {
        fun createRoute(sourceId: String, videoId: String): String {
            return "play/$sourceId/$videoId"
        }
    }
    object Favorites : Screen("favorites")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Cache : Screen("cache")
}
