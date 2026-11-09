package dev.erolit.reader.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.toApiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedPage<T>(val items: List<T>, val hasMore: Boolean)

data class FeedState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: ApiError? = null,
    val endReached: Boolean = false,
    val page: Int = 0,
    val started: Boolean = false,
)

/** Sequential paged list loader. One request at a time; pages are appended in order. */
class PagedFeed<T>(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(FeedState<T>())
    val state: StateFlow<FeedState<T>> = _state.asStateFlow()
    private var source: (suspend (Int) -> FeedPage<T>)? = null
    private var job: Job? = null

    val isStarted: Boolean get() = _state.value.started

    fun start(newSource: suspend (Int) -> FeedPage<T>) {
        source = newSource
        reload()
    }

    fun reload() {
        val src = source ?: return
        job?.cancel()
        _state.value = FeedState(loading = true, started = true)
        job = scope.launch { load(src, 1, emptyList()) }
    }

    fun loadMore() {
        val src = source ?: return
        val current = _state.value
        if (current.loading || current.loadingMore || current.endReached || current.error != null) return
        _state.update { it.copy(loadingMore = true) }
        job = scope.launch { load(src, current.page + 1, current.items) }
    }

    fun retry() {
        if (_state.value.items.isEmpty()) {
            reload()
        } else {
            _state.update { it.copy(error = null) }
            loadMore()
        }
    }

    fun removeWhere(predicate: (T) -> Boolean) {
        _state.update { it.copy(items = it.items.filterNot(predicate)) }
    }

    private suspend fun load(src: suspend (Int) -> FeedPage<T>, page: Int, previous: List<T>) {
        try {
            val result = src(page)
            _state.value = FeedState(
                items = previous + result.items,
                endReached = !result.hasMore || result.items.isEmpty(),
                page = page,
                started = true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _state.value = FeedState(
                items = previous,
                error = e.toApiError(),
                page = page - 1,
                started = true,
            )
        }
    }
}

fun <V : ViewModel> vmFactory(builder: () -> V): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
    }
