package com.erolit.app.ui.screen.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erolit.app.domain.model.StoryPage
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.LibraryRepository
import com.erolit.app.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReaderTheme { LIGHT, DARK, SEPIA, AMOLED }

data class ReaderUiState(
    val story: Story? = null,
    val pages: List<StoryPage> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val scrollFraction: Float = 0f,
    val fontSize: Float = 18f,
    val lineSpacing: Float = 1.8f,
    val readerTheme: ReaderTheme = ReaderTheme.DARK,
    val isSaved: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadedPageNumbers: Set<Int> = emptySet(),
    val showSettingsSheet: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isReadingMode: Boolean = false,
    val readPageNumbers: Set<Int> = emptySet()
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** Debounced scroll-save job — cancelled on every new scroll event. */
    private var scrollSaveJob: Job? = null

    fun init(slug: String) {
        viewModelScope.launch {
            // Check if offline first
            if (libraryRepository.isDownloaded(slug)) {
                loadOffline(slug)
                return@launch
            }
            loadStory(slug)
        }
    }

    /**
     * Force a network refresh regardless of offline/download state.
     * Wired to the Refresh icon in ReaderScreen so it always fetches fresh data.
     */
    fun refreshFromNetwork(slug: String) {
        viewModelScope.launch { loadStory(slug) }
    }

    private fun loadStory(slug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load saved/download status
            val isSaved = libraryRepository.isStorySaved(slug)
            val isDownloaded = libraryRepository.isDownloaded(slug)
            val progress = libraryRepository.getReadingProgress(slug)

            // Load first page details
            val downloadedPages = libraryRepository.getDownloadedPages(slug)
            val downloadedPageNums = downloadedPages.map { it.pageNumber }.toSet()
            
            // Observe read pages
            libraryRepository.getReadPages(slug).onEach { readPages ->
                _uiState.update { it.copy(readPageNumbers = readPages.toSet()) }
            }.launchIn(viewModelScope)

            storyRepository.getStoryDetail(slug).fold(
                onSuccess = { story ->
                    _uiState.update {
                        it.copy(
                            story = story,
                            totalPages = story.pageCount,
                            isSaved = isSaved,
                            isDownloaded = isDownloaded,
                            downloadedPageNumbers = downloadedPageNums,
                            currentPage = progress?.second ?: 1,
                            scrollFraction = progress?.first ?: 0f
                        )
                    }
                    loadPage(slug, progress?.second ?: 1)
                    // Mark overall story as read (history)
                    libraryRepository.markAsRead(story)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun loadPage(slug: String, page: Int) {
        if (_uiState.value.isOfflineMode) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val offlinePages = libraryRepository.getDownloadedPages(slug)
                val target = offlinePages.find { it.pageNumber == page }
                if (target != null) {
                    _uiState.update { it.copy(pages = listOf(target), currentPage = page, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Page not found offline") }
                }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            storyRepository.getStoryPage(slug, page).fold(
                onSuccess = { storyPage ->
                    _uiState.update { state ->
                        state.copy(
                            pages = listOf(storyPage),
                            currentPage = page,
                            totalPages = storyPage.totalPages,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    private fun loadOffline(slug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val story = libraryRepository.getStory(slug)
            val pages = libraryRepository.getDownloadedPages(slug)
            val progress = libraryRepository.getReadingProgress(slug)
            val targetPage = progress?.second ?: 1
            val pageToDisplay = pages.find { it.pageNumber == targetPage } ?: pages.firstOrNull()
            
            _uiState.update {
                it.copy(
                    story = story,
                    pages = pageToDisplay?.let { p -> listOf(p) } ?: emptyList(),
                    totalPages = story?.pageCount ?: pages.size,
                    isLoading = false,
                    isOfflineMode = true,
                    currentPage = progress?.second ?: 1,
                    scrollFraction = progress?.first ?: 0f,
                    isDownloaded = true,
                    downloadedPageNumbers = pages.map { it.pageNumber }.toSet(),
                    isSaved = true // Downloaded stories are usually saved conceptually
                )
            }
            // Mark as read/logged
            story?.let { libraryRepository.markAsRead(it) }

            // Observe read pages
            libraryRepository.getReadPages(slug).onEach { readPages ->
                _uiState.update { it.copy(readPageNumbers = readPages.toSet()) }
            }.launchIn(viewModelScope)
        }
    }

    fun onPageVisible(page: Int, slug: String) {
        if (uiState.value.currentPage == page) return
        _uiState.update { it.copy(currentPage = page) }
        viewModelScope.launch {
            libraryRepository.saveReadingProgress(slug, uiState.value.scrollFraction, page)
        }
    }

    fun onScrollFractionChanged(fraction: Float, slug: String) {
        _uiState.update { it.copy(scrollFraction = fraction) }
        // Debounce: cancel any pending save and schedule a new one 500ms out.
        // This prevents dozens of DB writes per second during smooth scrolling.
        scrollSaveJob?.cancel()
        scrollSaveJob = viewModelScope.launch {
            delay(500)
            libraryRepository.saveReadingProgress(slug, fraction, uiState.value.currentPage)
        }
    }

    fun toggleSave(story: Story) {
        viewModelScope.launch {
            if (uiState.value.isSaved) {
                libraryRepository.unsaveStory(story.slug)
                _uiState.update { it.copy(isSaved = false) }
            } else {
                libraryRepository.saveStory(story)
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun downloadStory(slug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val story = uiState.value.story
            storyRepository.getAllStoryPages(slug).fold(
                onSuccess = { pages ->
                    // Save story metadata first, so it appears in Downloads list
                    if (story != null) {
                        libraryRepository.saveStory(story)
                    }
                    libraryRepository.saveDownloadedPages(slug, pages)
                    _uiState.update { 
                        it.copy(
                            isDownloaded = true, 
                            downloadedPageNumbers = pages.map { p -> p.pageNumber }.toSet(),
                            isLoading = false
                        ) 
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Download failed: ${e.message}") }
                }
            )
        }
    }

    fun downloadPage(slug: String, page: Int) {
        viewModelScope.launch {
            storyRepository.getStoryPage(slug, page).fold(
                onSuccess = { storyPage ->
                    val story = uiState.value.story
                    if (story != null) {
                        libraryRepository.saveStory(story)
                    }
                    libraryRepository.saveDownloadedPages(slug, listOf(storyPage))
                    _uiState.update { 
                        it.copy(
                            isDownloaded = true,
                            downloadedPageNumbers = it.downloadedPageNumbers + storyPage.pageNumber
                        ) 
                    }
                },
                onFailure = {} // Silently fail or track state
            )
        }
    }

    fun refreshStory() {
        uiState.value.story?.slug?.let { init(it) }
    }

    fun setFontSize(size: Float) = _uiState.update { it.copy(fontSize = size) }
    fun setReaderTheme(theme: ReaderTheme) = _uiState.update { it.copy(readerTheme = theme) }
    fun setLineSpacing(spacing: Float) = _uiState.update { it.copy(lineSpacing = spacing) }
    fun toggleSettingsSheet() = _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }

    fun startReading() = _uiState.update { it.copy(isReadingMode = true) }
    fun exitReadingMode() = _uiState.update { it.copy(isReadingMode = false) }

    fun jumpToPage(page: Int) {
        val slug = uiState.value.story?.slug ?: return
        _uiState.update { 
            it.copy(
                isReadingMode = true, 
                currentPage = page,
                pages = emptyList(), // Clear existing pages
                scrollFraction = 0f // Start at the top of the new page
            ) 
        }
        loadPage(slug, page)
    }

    fun retryLoadPage() {
        val slug = uiState.value.story?.slug ?: return
        loadPage(slug, uiState.value.currentPage)
    }

    fun markPageAsRead(pageNumber: Int) {
        val slug = uiState.value.story?.slug ?: return
        viewModelScope.launch {
            libraryRepository.markPageAsRead(slug, pageNumber)
        }
    }

    fun markPageAsUnread(pageNumber: Int) {
        val slug = uiState.value.story?.slug ?: return
        viewModelScope.launch {
            libraryRepository.markPageAsUnread(slug, pageNumber)
        }
    }
}
