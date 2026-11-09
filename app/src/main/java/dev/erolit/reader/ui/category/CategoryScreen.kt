package dev.erolit.reader.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.data.repo.CatalogRepository
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.common.FeedPage
import dev.erolit.reader.ui.common.PagedFeed
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.components.FeedList
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.StoryCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CategoryViewModel(
    private val catalog: CatalogRepository,
    private val categoryId: Int,
) : ViewModel() {
    val feed = PagedFeed<StorySummary>(viewModelScope)

    private val _mode = MutableStateFlow(MODE_NEW)
    val mode: StateFlow<String> = _mode.asStateFlow()

    private val _period = MutableStateFlow<String?>(null)
    val period: StateFlow<String?> = _period.asStateFlow()

    fun ensureStarted() {
        if (feed.isStarted) return
        reload()
    }

    fun setMode(mode: String) {
        if (_mode.value == mode) return
        _mode.value = mode
        reload()
    }

    fun setPeriod(period: String?) {
        if (_period.value == period) return
        _period.value = period
        reload()
    }

    private fun reload() {
        val mode = _mode.value
        val period = _period.value
        feed.start { page ->
            val result = if (mode == MODE_POPULAR) {
                catalog.categoryTopStories(page, categoryId, period)
            } else {
                catalog.categoryStories(page, categoryId)
            }
            FeedPage(
                items = result.items,
                hasMore = result.items.isNotEmpty() && page * result.pageSize < result.total,
            )
        }
    }

    companion object {
        const val MODE_NEW = "new"
        const val MODE_POPULAR = "popular"
    }
}

@Composable
fun CategoryScreen(
    container: AppContainer,
    categoryId: Int,
    name: String,
    onBack: () -> Unit,
    onStory: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val vm: CategoryViewModel = viewModel(
        key = "category-$categoryId",
        factory = vmFactory { CategoryViewModel(container.catalog, categoryId) },
    )
    LaunchedEffect(categoryId) { vm.ensureStarted() }
    val state by vm.feed.state.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = name, onBack = onBack)
        ChipRow {
            Chip("New", mode == CategoryViewModel.MODE_NEW) {
                vm.setMode(CategoryViewModel.MODE_NEW)
            }
            Chip("Popular", mode == CategoryViewModel.MODE_POPULAR) {
                vm.setMode(CategoryViewModel.MODE_POPULAR)
            }
        }
        if (mode == CategoryViewModel.MODE_POPULAR) {
            ChipRow {
                Chip("7 days", period == "week") { vm.setPeriod("week") }
                Chip("30 days", period == "month") { vm.setPeriod("month") }
                Chip("12 months", period == "year") { vm.setPeriod("year") }
                Chip("All time", period == null) { vm.setPeriod(null) }
            }
        }
        FeedList(
            state = state,
            onLoadMore = { vm.feed.loadMore() },
            onRetry = { vm.feed.retry() },
            emptyText = if (mode == CategoryViewModel.MODE_POPULAR) {
                "No popular stories in this category."
            } else {
                "No stories in this category."
            },
        ) { story ->
            StoryCard(
                story = story,
                onClick = { onStory(story.id) },
                onAuthor = story.authorId?.let { id -> { onAuthor(id) } },
            )
        }
    }
}
