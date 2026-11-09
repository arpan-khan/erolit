package dev.erolit.reader.ui.common

import dev.erolit.reader.core.net.ApiError

/** Shared UI state for every list screen. */
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Content<T>(val data: T, val paging: PagingInfo? = null) : UiState<T>
    object Empty : UiState<Nothing>
    data class Error(val error: ApiError) : UiState<Nothing>
}

data class PagingInfo(
    val page: Int,
    val total: Int,
    val pageSize: Int,
    val endReached: Boolean,
)
