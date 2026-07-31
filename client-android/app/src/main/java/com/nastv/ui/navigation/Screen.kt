package com.nastv.ui.navigation

sealed class Screen(val route: String) {
    object ServerSelect : Screen("server_select")
    object Login : Screen("login/{serverId}") {
        fun createRoute(serverId: String): String {
            return "login/$serverId"
        }
    }
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
    object TvSearchResult : Screen("tv_search_result/{keyword}") {
        fun createRoute(keyword: String): String {
            return "tv_search_result/$keyword"
        }
    }
}
