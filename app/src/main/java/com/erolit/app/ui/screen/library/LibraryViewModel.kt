package com.erolit.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.ReadingList
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val savedStories: List<Story> = emptyList(),
    val recentlyRead: List<Story> = emptyList(),
    val downloadedStories: List<Story> = emptyList(),
    val readingLists: List<ReadingList> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            combine(
                libraryRepository.getSavedStories(),
                libraryRepository.getRecentlyRead(),
                libraryRepository.getDownloadedStories(),
                libraryRepository.getAllReadingLists()
            ) { saved, recent, downloaded, lists ->
                LibraryUiState(saved, recent, downloaded, lists, isLoading = false)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createReadingList(name: String) = viewModelScope.launch {
        libraryRepository.createReadingList(name)
    }

    fun deleteList(id: String) = viewModelScope.launch {
        libraryRepository.deleteReadingList(id)
    }

    fun unsaveStory(slug: String) = viewModelScope.launch {
        libraryRepository.unsaveStory(slug)
    }

    fun deleteDownload(slug: String) = viewModelScope.launch {
        libraryRepository.deleteDownload(slug)
    }
}
