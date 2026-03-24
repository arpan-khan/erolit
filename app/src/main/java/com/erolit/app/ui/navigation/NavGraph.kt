package com.erolit.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.erolit.app.ui.screen.author.AuthorScreen
import com.erolit.app.ui.screen.browse.BrowseScreen
import com.erolit.app.ui.screen.browse.CategoryDetailScreen
import com.erolit.app.ui.screen.browse.SeriesDetailScreen
import com.erolit.app.ui.screen.browse.TagDetailScreen
import com.erolit.app.ui.screen.home.HomeScreen
import com.erolit.app.ui.screen.library.LibraryScreen
import com.erolit.app.ui.screen.reader.ReaderScreen
import com.erolit.app.ui.screen.search.SearchScreen
import com.erolit.app.ui.screen.settings.SettingsScreen
import com.erolit.app.ui.screen.login.LoginScreen

import androidx.compose.ui.Modifier

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        modifier = modifier,
        startDestination = Screen.Home.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(Screen.Library.route) {
            LibraryScreen(navController = navController)
        }
        composable(Screen.Browse.route) {
            BrowseScreen(navController = navController)
        }
        composable(Screen.Reader.route) { backStack ->
            val slug = backStack.arguments?.getString("slug") ?: return@composable
            ReaderScreen(slug = slug, navController = navController)
        }
        composable(Screen.CategoryDetail.route) { backStack ->
            val slug = backStack.arguments?.getString("slug") ?: return@composable
            CategoryDetailScreen(categorySlug = slug, navController = navController)
        }
        composable(Screen.AuthorProfile.route) { backStack ->
            val username = backStack.arguments?.getString("username") ?: return@composable
            AuthorScreen(username = username, navController = navController)
        }
        composable(Screen.SeriesDetail.route) { backStack ->
            val seriesId = backStack.arguments?.getString("seriesId") ?: return@composable
            SeriesDetailScreen(seriesId = seriesId, navController = navController)
        }
        composable(Screen.TagDetail.route) { backStack ->
            val tagName = backStack.arguments?.getString("tagName") ?: ""
            TagDetailScreen(tagName = tagName, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(
            route = Screen.WebView.route,
            arguments = listOf(androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType })
        ) { backStack ->
            val url = backStack.arguments?.getString("url")?.let { android.net.Uri.decode(it) } ?: "https://www.literotica.com"
            com.erolit.app.ui.screen.webview.WebViewScreen(url = url, navController = navController)
        }
    }
}
