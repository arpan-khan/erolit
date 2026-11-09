package dev.erolit.reader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.core.settings.SettingsRepository
import dev.erolit.reader.data.repo.CatalogRepository
import dev.erolit.reader.domain.HomeTab
import dev.erolit.reader.domain.PagedStories
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.common.FeedPage
import dev.erolit.reader.ui.common.PagedFeed
import dev.erolit.reader.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val catalog: CatalogRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val newFeed = PagedFeed<StorySummary>(viewModelScope)
    val topFeed = PagedFeed<StorySummary>(viewModelScope)

    private val _tab = MutableStateFlow(HomeTab.New)
    val tab: StateFlow<HomeTab> = _tab.asStateFlow()

    private val _topSort = MutableStateFlow("rating")
    val topSort: StateFlow<String> = _topSort.asStateFlow()

    private val _topPeriod = MutableStateFlow<String?>("week")
    val topPeriod: StateFlow<String?> = _topPeriod.asStateFlow()

    private val _seriesKind = MutableStateFlow("recently-updated")
    val seriesKind: StateFlow<String> = _seriesKind.asStateFlow()

    private val _series = MutableStateFlow<UiState<List<SeriesSummary>>>(UiState.Loading)
    val series: StateFlow<UiState<List<SeriesSummary>>> = _series.asStateFlow()
    private var seriesLoaded = false

    init {
        viewModelScope.launch {
            val s = runCatching { settings.current() }.getOrNull()
            if (s != null) {
                _tab.value = when (s.defaultHomeTab) {
                    "top" -> HomeTab.Top
                    "series" -> HomeTab.Series
                    else -> HomeTab.New
                }
                _topSort.value = s.defaultTopSort
                _topPeriod.value = s.defaultTopPeriod.takeIf { it != "all" && it.isNotBlank() }
            }
            ensureLoaded()
        }
    }

    fun selectTab(tab: HomeTab) {
        _tab.value = tab
        ensureLoaded()
    }

    private fun ensureLoaded() {
        when (_tab.value) {
            HomeTab.New -> if (!newFeed.isStarted) startNew()
            HomeTab.Top -> if (!topFeed.isStarted) startTop()
            HomeTab.Series -> if (!seriesLoaded) loadSeries()
        }
    }

    private fun toPage(page: Int, result: PagedStories) = FeedPage(
        items = result.items,
        hasMore = result.items.isNotEmpty() && page * result.pageSize < result.total,
    )

    private fun startNew() {
        newFeed.start { page -> toPage(page, catalog.newStories(page)) }
    }

    private fun startTop() {
        val sort = _topSort.value
        val period = _topPeriod.value
        topFeed.start { page -> toPage(page, catalog.topStories(page, sort, period)) }
    }

    fun setTopSort(sort: String) {
        _topSort.value = sort
        startTop()
    }

    fun setTopPeriod(period: String?) {
        _topPeriod.value = period
        startTop()
    }

    fun setSeriesKind(kind: String) {
        _seriesKind.value = kind
        loadSeries()
    }

    fun loadSeries() {
        seriesLoaded = true
        _series.value = UiState.Loading
        val kind = _seriesKind.value
        viewModelScope.launch {
            try {
                val result = catalog.seriesFeed(kind)
                _series.value = if (result.isEmpty()) UiState.Empty else UiState.Content(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _series.value = UiState.Error(e.toApiError())
            }
        }
    }
}
