package dev.erolit.reader.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.domain.HomeTab
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.FeedList
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.SeriesCard
import dev.erolit.reader.ui.components.StoryCard

@Composable
fun HomeScreen(
    container: AppContainer,
    onStory: (Long) -> Unit,
    onSeries: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val vm: HomeViewModel = viewModel(
        factory = vmFactory { HomeViewModel(container.catalog, container.settings) },
    )
    val tab by vm.tab.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Discover", onBack = null)
        TabRow(selectedTabIndex = tab.ordinal) {
            HomeTab.values().forEach { t ->
                Tab(
                    selected = t == tab,
                    onClick = { vm.selectTab(t) },
                    text = { Text(t.name) },
                )
            }
        }
        Crossfade(
            targetState = tab,
            animationSpec = tween(durationMillis = 180),
            label = "home-tab",
            modifier = Modifier.weight(1f),
        ) { current ->
            Column(modifier = Modifier.fillMaxSize()) {
            when (current) {
            HomeTab.New -> {
                val state by vm.newFeed.state.collectAsStateWithLifecycle()
                FeedList(
                    state = state,
                    onLoadMore = { vm.newFeed.loadMore() },
                    onRetry = { vm.newFeed.retry() },
                    emptyText = "No stories found.",
                ) { story ->
                    StoryCard(story = story, onClick = { onStory(story.id) })
                }
            }
            HomeTab.Top -> {
                val state by vm.topFeed.state.collectAsStateWithLifecycle()
                val sort by vm.topSort.collectAsStateWithLifecycle()
                val period by vm.topPeriod.collectAsStateWithLifecycle()
                ChipRow {
                    Chip("Top rated", sort == "rating") { vm.setTopSort("rating") }
                    Chip("Most read", sort == "views") { vm.setTopSort("views") }
                    Chip("Favorites", sort == "favorite") { vm.setTopSort("favorite") }
                }
                ChipRow {
                    Chip("7 days", period == "week") { vm.setTopPeriod("week") }
                    Chip("30 days", period == "month") { vm.setTopPeriod("month") }
                    Chip("12 months", period == "year") { vm.setTopPeriod("year") }
                    Chip("All time", period == null) { vm.setTopPeriod(null) }
                }
                FeedList(
                    state = state,
                    onLoadMore = { vm.topFeed.loadMore() },
                    onRetry = { vm.topFeed.retry() },
                    emptyText = "No stories found.",
                ) { story ->
                    StoryCard(story = story, onClick = { onStory(story.id) })
                }
            }
            HomeTab.Series -> {
                val series by vm.series.collectAsStateWithLifecycle()
                val kind by vm.seriesKind.collectAsStateWithLifecycle()
                ChipRow {
                    Chip("Updated", kind == "recently-updated") { vm.setSeriesKind("recently-updated") }
                    Chip("Discussed", kind == "most-discussed") { vm.setSeriesKind("most-discussed") }
                    Chip("Favorited", kind == "most-favorited") { vm.setSeriesKind("most-favorited") }
                    Chip("Started", kind == "recently-started") { vm.setSeriesKind("recently-started") }
                    Chip("Longest running", kind == "longest-running") { vm.setSeriesKind("longest-running") }
                }
                when (val s = series) {
                    UiState.Loading -> LoadingBox()
                    UiState.Empty -> EmptyBox("No series found.")
                    is UiState.Error -> ErrorBox(s.error, onRetry = { vm.loadSeries() })
                    is UiState.Content -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        itemsIndexed(s.data) { _, item ->
                            SeriesCard(
                                series = item,
                                onClick = { onSeries(item.id) },
                                onAuthor = item.authorId?.let { id -> { onAuthor(id) } },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
}
