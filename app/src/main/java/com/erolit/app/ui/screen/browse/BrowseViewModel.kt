package com.erolit.app.ui.screen.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.Series
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val categoryStories: List<Story> = emptyList(),
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel()
