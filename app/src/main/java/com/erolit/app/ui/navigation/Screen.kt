package com.erolit.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Browse : Screen("browse")

    data object Reader : Screen("reader/{slug}") {
        fun createRoute(slug: String) = "reader/$slug"
    }
    data object CategoryDetail : Screen("category/{slug}") {
        fun createRoute(slug: String) = "category/$slug"
    }
    data object AuthorProfile : Screen("author/{username}") {
        fun createRoute(username: String) = "author/$username"
    }
    data object SeriesDetail : Screen("series/{seriesId}") {
        fun createRoute(seriesId: String) = "series/$seriesId"
    }
    data object TagDetail : Screen("tag/{tagName}") {
        fun createRoute(tagName: String) = "tag/$tagName"
    }

    data object Settings : Screen("settings")
    data object Login : Screen("login")
    
    data object WebView : Screen("webview?url={url}") {
        fun createRoute(url: String) = "webview?url=${android.net.Uri.encode(url)}"
    }
}
