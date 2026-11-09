package dev.erolit.reader.ui.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.data.repo.SeriesRepository
import dev.erolit.reader.domain.SeriesDetail
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.StoryCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeriesViewModel(
    private val repo: SeriesRepository,
    private val seriesId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<SeriesDetail>>(UiState.Loading)
    val state: StateFlow<UiState<SeriesDetail>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val detail = repo.load(seriesId.toString())
                _state.value = if (detail.works.isEmpty()) UiState.Empty else UiState.Content(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Error(e.toApiError())
            }
        }
    }
}

@Composable
fun SeriesScreen(
    container: AppContainer,
    seriesId: Long,
    onBack: () -> Unit,
    onStory: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val vm: SeriesViewModel = viewModel(
        key = "series-$seriesId",
        factory = vmFactory { SeriesViewModel(container.series, seriesId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        val title = (state as? UiState.Content<SeriesDetail>)?.data?.title ?: "Series"
        ScreenHeader(title = title, onBack = onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            UiState.Empty -> EmptyBox("This series has no parts yet.")
            is UiState.Error -> ErrorBox(s.error, onRetry = { vm.load() })
            is UiState.Content -> {
                val detail = s.data
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            val meta = listOfNotNull(
                                detail.authorName,
                                "${detail.workCount} parts",
                                detail.state?.replace('_', ' '),
                            ).joinToString(" · ")
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val description = detail.description
                            if (!description.isNullOrBlank()) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 6,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            val authorId = detail.authorId
                            if (authorId != null) {
                                TextButton(onClick = { onAuthor(authorId) }) {
                                    Text("More by ${detail.authorName ?: "this author"}")
                                }
                            }
                        }
                    }
                    itemsIndexed(detail.works) { _, story ->
                        StoryCard(story = story, onClick = { onStory(story.id) })
                    }
                }
            }
        }
    }
}
