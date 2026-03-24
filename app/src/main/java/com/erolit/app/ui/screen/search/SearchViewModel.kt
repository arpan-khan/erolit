package com.erolit.app.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Story> = emptyList(),
    val history: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        // Collect search history
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
        // Debounced search
        viewModelScope.launch {
            queryFlow
                .debounce { if (it.isBlank()) 0L else 400L }
                .filter { it.length >= 2 || it.isBlank() }
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    if (q.isBlank()) flowOf(Result.success(emptyList<Story>()))
                    else {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                        searchRepository.search(q)
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { stories -> _uiState.update { it.copy(results = stories, isLoading = false) } },
                        onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false, results = emptyList()) } }
                    )
                }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        queryFlow.value = q
    }

    fun onSearch(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            searchRepository.addToHistory(q)
            queryFlow.emit(q) // Emit again to ensure search if it's the same q
        }
    }

    fun clearHistory() = viewModelScope.launch { searchRepository.clearHistory() }
}
