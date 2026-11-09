package dev.erolit.reader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.erolit.reader.data.repo.SearchRepository
import dev.erolit.reader.domain.MemberSummary
import dev.erolit.reader.domain.SearchResult
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.common.FeedPage
import dev.erolit.reader.ui.common.PagedFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(private val search: SearchRepository) : ViewModel() {

    val stories = PagedFeed<StorySummary>(viewModelScope)
    val members = PagedFeed<MemberSummary>(viewModelScope)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _type = MutableStateFlow("stories")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _sort = MutableStateFlow<String?>(null)
    val sort: StateFlow<String?> = _sort.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    val history: StateFlow<List<String>> = search.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setType(value: String) {
        _type.value = value
        if (_submitted.value) run()
    }

    fun setSort(value: String?) {
        _sort.value = value
        if (_submitted.value && _type.value != "members") run()
    }

    fun useHistory(value: String) {
        _query.value = value
        run()
    }

    fun clearHistory() {
        viewModelScope.launch { search.clearHistory() }
    }

    fun run() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        _submitted.value = true
        val sortValue = _sort.value
        when (_type.value) {
            "members" -> members.start { page ->
                val r = search.searchMembers(q, page)
                FeedPage(r.members, r.members.isNotEmpty() && page * r.pageSize < r.total)
            }
            "poems" -> stories.start { page ->
                storyPage(page, search.searchPoems(q, page, emptyList(), sortValue))
            }
            else -> stories.start { page ->
                storyPage(page, search.searchStories(q, page, emptyList(), sortValue))
            }
        }
    }

    private fun storyPage(page: Int, r: SearchResult) =
        FeedPage(r.stories, r.stories.isNotEmpty() && page * r.pageSize < r.total)
}
