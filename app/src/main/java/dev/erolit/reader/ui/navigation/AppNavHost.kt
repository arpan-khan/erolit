package dev.erolit.reader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.erolit.reader.AppContainer
import dev.erolit.reader.R
import dev.erolit.reader.ui.author.AuthorScreen
import dev.erolit.reader.ui.categories.CategoriesScreen
import dev.erolit.reader.ui.category.CategoryScreen
import dev.erolit.reader.ui.common.LocalSavedStoryIds
import dev.erolit.reader.ui.common.LocalStoryNavigation
import dev.erolit.reader.ui.common.LocalStoryProgress
import dev.erolit.reader.ui.common.StoryNavigation
import dev.erolit.reader.ui.home.HomeScreen
import dev.erolit.reader.ui.lists.ListDetailScreen
import dev.erolit.reader.ui.lists.MyListsScreen
import dev.erolit.reader.ui.reader.ReaderScreen
import dev.erolit.reader.ui.search.SearchScreen
import dev.erolit.reader.ui.series.SeriesScreen
import dev.erolit.reader.ui.settings.SettingsScreen

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.Home
    val progressFlow = remember(container) { container.stories.observeProgress() }
    val storyProgress by progressFlow.collectAsStateWithLifecycle(
        initialValue = emptyMap<Long, dev.erolit.reader.domain.ProgressInfo>(),
    )
    val savedFlow = remember(container) { container.stories.observeLocalSaved() }
    val savedStoryIds by savedFlow.collectAsStateWithLifecycle(
        initialValue = emptyList<dev.erolit.reader.domain.StorySummary>(),
    )
    val savedIds = remember(savedStoryIds) { savedStoryIds.map { it.id }.toSet() }

    val storyNavigation = remember(navController) {
        StoryNavigation(
            openAuthor = { id -> navController.navigate(Routes.author(id)) },
            openSeries = { id -> navController.navigate(Routes.series(id)) },
            openCategory = { id, name -> navController.navigate(Routes.category(id, name)) },
            openTag = { tag -> if (tag.isNotBlank()) navController.navigate(Routes.search(tag)) },
        )
    }

    var lastTab by remember { mutableStateOf(0) }
    val tabIndex = when {
        route == Routes.Home -> 0
        route == Routes.Categories || route.startsWith("category/") -> 1
        route.startsWith("search") -> 2
        route == Routes.Lists || route.startsWith("list/") -> 3
        route == Routes.Settings -> 4
        else -> null
    }
    SideEffect { if (tabIndex != null) lastTab = tabIndex }
    val showBar = !route.startsWith("story/")

    val tabs = listOf(
        TabItem(Routes.Home, stringResource(R.string.tab_home), Icons.Default.Home),
        TabItem(Routes.Categories, stringResource(R.string.tab_categories), Icons.Default.Menu),
        TabItem(Routes.search(), stringResource(R.string.tab_search), Icons.Default.Search),
        TabItem(Routes.Lists, stringResource(R.string.tab_lists), Icons.Default.Person),
        TabItem(Routes.Settings, stringResource(R.string.tab_settings), Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = (tabIndex ?: lastTab) == index,
                            onClick = { navigateToTab(navController, tab.route) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalStoryProgress provides storyProgress,
            LocalSavedStoryIds provides savedIds,
            LocalStoryNavigation provides storyNavigation,
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.Home,
                modifier = Modifier.padding(innerPadding),
            ) {
            composable(Routes.Home) {
                HomeScreen(
                    container = container,
                    onStory = { navController.navigate(Routes.story(it)) },
                    onSeries = { navController.navigate(Routes.series(it)) },
                    onAuthor = { navController.navigate(Routes.author(it)) },
                )
            }
            composable(Routes.Categories) {
                CategoriesScreen(container) { id, name ->
                    navController.navigate(Routes.category(id, name))
                }
            }
            composable(
                route = Routes.Category,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.IntType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                CategoryScreen(
                    container = container,
                    categoryId = entry.arguments?.getInt("categoryId") ?: 0,
                    name = entry.arguments?.getString("name") ?: "Category",
                    onBack = { navController.popBackStack() },
                    onStory = { navController.navigate(Routes.story(it)) },
                )
            }
            composable(
                route = Routes.Search,
                arguments = listOf(
                    navArgument("q") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                SearchScreen(
                    container = container,
                    initialQuery = entry.arguments?.getString("q"),
                    onStory = { navController.navigate(Routes.story(it)) },
                    onAuthor = { navController.navigate(Routes.author(it)) },
                )
            }
            composable(Routes.Lists) {
                MyListsScreen(
                    container = container,
                    onList = { navController.navigate(Routes.list(it)) },
                    onStory = { navController.navigate(Routes.story(it)) },
                    onSeries = { navController.navigate(Routes.series(it)) },
                )
            }
            composable(
                route = Routes.ListDetail,
                arguments = listOf(navArgument("urlname") { type = NavType.StringType }),
            ) { entry ->
                ListDetailScreen(
                    container = container,
                    urlname = entry.arguments?.getString("urlname") ?: "",
                    onBack = { navController.popBackStack() },
                    onStory = { navController.navigate(Routes.story(it)) },
                )
            }
            composable(Routes.Settings) { SettingsScreen(container) }
            composable(
                route = Routes.Story,
                arguments = listOf(navArgument("storyId") { type = NavType.LongType }),
            ) { entry ->
                ReaderScreen(
                    container = container,
                    storyId = entry.arguments?.getLong("storyId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.Series,
                arguments = listOf(navArgument("seriesId") { type = NavType.LongType }),
            ) { entry ->
                SeriesScreen(
                    container = container,
                    seriesId = entry.arguments?.getLong("seriesId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onStory = { navController.navigate(Routes.story(it)) },
                    onAuthor = { navController.navigate(Routes.author(it)) },
                )
            }
            composable(
                route = Routes.Author,
                arguments = listOf(navArgument("authorId") { type = NavType.LongType }),
            ) { entry ->
                AuthorScreen(
                    container = container,
                    authorId = entry.arguments?.getLong("authorId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onStory = { navController.navigate(Routes.story(it)) },
                )
            }
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
