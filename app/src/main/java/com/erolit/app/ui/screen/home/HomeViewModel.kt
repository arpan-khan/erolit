package com.erolit.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentlyRead: List<Story> = emptyList(),
    val newStories: List<Story> = emptyList(),
    val topRated: List<Story> = emptyList(),
    val mostRead: List<Story> = emptyList(),
    val isLoadingNew: Boolean = false,
    val isLoadingTop: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val libraryRepository: com.erolit.app.domain.repository.LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        observeRecentlyRead()
    }

    private fun observeRecentlyRead() {
        viewModelScope.launch {
            libraryRepository.getRecentlyRead().collect { stories ->
                _uiState.update { it.copy(recentlyRead = stories) }
            }
        }
    }

    fun loadAll() {
        loadNewStories()
        loadTopRated()
    }

    private fun loadNewStories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNew = true, error = null) }
            storyRepository.getNewStories()
                .catch { e -> _uiState.update { it.copy(isLoadingNew = false, error = e.message) } }
                .collect { result ->
                    result.fold(
                        onSuccess = { stories ->
                            _uiState.update { it.copy(newStories = stories, isLoadingNew = false) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoadingNew = false, error = e.message) }
                        }
                    )
                }
        }
    }

    private fun loadTopRated() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTop = true) }
            storyRepository.getTopRatedStories()
                .catch { e -> _uiState.update { it.copy(isLoadingTop = false) } }
                .collect { result ->
                    result.fold(
                        onSuccess = { stories ->
                            _uiState.update { it.copy(topRated = stories, isLoadingTop = false) }
                        },
                        onFailure = { _uiState.update { it.copy(isLoadingTop = false) } }
                    )
                }
        }
    }

    fun retry() = loadAll()
}
