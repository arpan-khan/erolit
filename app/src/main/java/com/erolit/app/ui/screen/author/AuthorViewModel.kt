package com.erolit.app.ui.screen.author

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.Author
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.AuthorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthorUiState(
    val author: Author? = null,
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthorViewModel @Inject constructor(
    private val authorRepository: AuthorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthorUiState())
    val uiState: StateFlow<AuthorUiState> = _uiState.asStateFlow()

    fun load(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authorRepository.getAuthor(username).fold(
                onSuccess = { author -> _uiState.update { it.copy(author = author, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
            authorRepository.getAuthorStories(username)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { result ->
                    result.onSuccess { stories -> _uiState.update { it.copy(stories = stories) } }
                }
        }
    }
}
