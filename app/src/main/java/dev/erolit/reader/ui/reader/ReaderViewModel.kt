package dev.erolit.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.data.repo.StoryRepository
import dev.erolit.reader.domain.StorySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderState(
    val loading: Boolean = true,
    val error: ApiError? = null,
    val title: String = "",
    val author: String? = null,
    val page: Int = 1,
    val pagesCount: Int = 1,
    val text: String = "",
    val restoreFraction: Float = 0f,
    val summary: StorySummary? = null,
    val savedLocally: Boolean = false,
)

class ReaderViewModel(
    private val stories: StoryRepository,
    private val storyId: Long,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastRequested = 1
    private var lastFraction = 0f

    /** History records the opening, not every page turn. */
    private var historyRecorded = false

    /**
     * Set once the reader has reached the end of the last page. From then on
     * every queued write is forced to 100%, so a late debounce or a teardown
     * save computed from a resized viewport cannot lower a finished chapter.
     */
    private var completed = false

    private data class PendingProgress(
        val page: Int,
        val fraction: Float,
        val pagesCount: Int,
    )

    /**
     * Progress writes are funnelled through one conflated queue. Without this,
     * a delayed debounce write from an earlier scroll position can land after
     * the final 100% write and make a finished chapter fall back to 99%.
     */
    private val pendingProgress = Channel<PendingProgress>(Channel.CONFLATED)
    private var latestProgress: PendingProgress? = null
    private val progressWriter = appScope.launch {
        for (request in pendingProgress) {
            try {
                stories.saveProgress(
                    storyId,
                    request.page,
                    request.fraction,
                    request.pagesCount,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Progress is best effort; never crash the reader over a DB write.
            }
        }
    }

    init {
        job = viewModelScope.launch {
            val saved = try {
                stories.progress(storyId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
            load(saved?.page ?: 1, saved?.scrollFraction ?: 0f)
        }
    }

    private suspend fun load(page: Int, fraction: Float) {
        val known = _state.value
        val wanted = if (!known.loading && known.text.isNotEmpty()) {
            page.coerceIn(1, known.pagesCount.coerceAtLeast(1))
        } else {
            page.coerceAtLeast(1)
        }
        lastRequested = wanted
        lastFraction = fraction
        _state.update { it.copy(loading = true, error = null) }
        try {
            val id = storyId.toString()
            val detail = try {
                stories.fetchPage(id, wanted)
            } catch (e: ApiError) {
                if (e === ApiError.NotFound && wanted > 1) stories.fetchPage(id, 1) else throw e
            }
            val restore = if (detail.page == wanted) fraction else 0f
            _state.value = ReaderState(
                loading = false,
                title = detail.summary.title,
                author = detail.summary.authorName,
                page = detail.page,
                pagesCount = detail.pagesCount.coerceAtLeast(1),
                text = detail.text,
                restoreFraction = restore,
                summary = detail.summary,
                savedLocally = stories.isLocalSaved(storyId),
            )
            recordOpened(detail.summary)
            enqueueProgress(detail.page, restore, detail.pagesCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Fall back to an already-downloaded page so the on-device list is
            // still readable when there is no connection.
            val cachedPage = stories.cachedPage(storyId, wanted)
            val cachedStory = stories.cachedStory(storyId)
            if (cachedPage != null && cachedStory != null && cachedPage.text.isNotEmpty()) {
                _state.value = ReaderState(
                    loading = false,
                    title = cachedStory.summary.title,
                    author = cachedStory.summary.authorName,
                    page = cachedPage.page,
                    pagesCount = cachedStory.pagesCount.coerceAtLeast(1),
                    text = cachedPage.text,
                    restoreFraction = fraction,
                    summary = cachedStory.summary,
                    savedLocally = stories.isLocalSaved(storyId),
                )
                recordOpened(cachedStory.summary)
            } else {
                _state.update { it.copy(loading = false, error = e.toApiError()) }
            }
        }
    }

    private fun go(page: Int, fraction: Float = 0f) {
        job?.cancel()
        job = viewModelScope.launch { load(page, fraction) }
    }

    private fun recordOpened(summary: StorySummary) {
        if (historyRecorded) return
        historyRecorded = true
        viewModelScope.launch { stories.recordHistory(summary) }
    }

    fun next() {
        val s = _state.value
        if (!s.loading && s.page < s.pagesCount) go(s.page + 1)
    }

    fun previous() {
        val s = _state.value
        if (!s.loading && s.page > 1) go(s.page - 1)
    }

    fun retry() = go(lastRequested, lastFraction)

    private fun enqueueProgress(page: Int, fraction: Float, pagesCount: Int) {
        val request = if (completed) {
            val pages = pagesCount.coerceAtLeast(1)
            PendingProgress(pages, 1f, pages)
        } else {
            PendingProgress(page, fraction, pagesCount)
        }
        latestProgress = request
        pendingProgress.trySend(request)
    }

    /** Marks the current chapter as finished and keeps it at 100% for this session. */
    fun markCompleted() {
        val s = _state.value
        if (s.loading || s.text.isEmpty() || s.error != null) return
        completed = true
        enqueueProgress(s.pagesCount, 1f, s.pagesCount)
    }

    fun saveScroll(fraction: Float) {
        val s = _state.value
        if (s.loading || s.text.isEmpty() || s.error != null) return
        if (completed) {
            enqueueProgress(s.pagesCount, 1f, s.pagesCount)
        } else {
            enqueueProgress(s.page, fraction, s.pagesCount)
        }
    }

    override fun onCleared() {
        // Make sure the final position is queued before the worker stops.
        latestProgress?.let { pendingProgress.trySend(it) }
        pendingProgress.close()
        super.onCleared()
    }

    fun saveLocally() {
        val summary = _state.value.summary ?: return
        viewModelScope.launch {
            stories.saveLocal(summary)
            _state.update { it.copy(savedLocally = true) }
        }
    }

    fun removeLocally() {
        viewModelScope.launch {
            stories.removeLocal(storyId)
            _state.update { it.copy(savedLocally = false) }
        }
    }
}
