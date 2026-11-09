package dev.erolit.reader.ui.home

import dev.erolit.reader.domain.HomeTab
import dev.erolit.reader.domain.PagedStories
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.ui.common.PagingInfo
import dev.erolit.reader.ui.common.UiState

data class HomeUiState(
    val tab: HomeTab = HomeTab.New,
    val new: UiState<PagedStories> = UiState.Loading,
    val top: UiState<PagedStories> = UiState.Loading,
    val series: UiState<List<SeriesSummary>> = UiState.Loading,
    val topSort: String = "rating",
    val topPeriod: String? = null,
    val seriesKind: String = "recently-updated",
    val newLoadingMore: Boolean = false,
    val topLoadingMore: Boolean = false,
)
