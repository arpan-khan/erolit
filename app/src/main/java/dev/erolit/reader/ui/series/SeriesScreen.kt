package dev.erolit.reader.ui.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.data.repo.SeriesRepository
import dev.erolit.reader.data.repo.StoryRepository
import dev.erolit.reader.domain.SeriesDetail
import dev.erolit.reader.ui.common.LocalStoryProgress
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.WebTarget
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.SeriesProgressBadge
import dev.erolit.reader.ui.components.StoryCard
import dev.erolit.reader.ui.components.WebButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SeriesViewModel(
    private val repo: SeriesRepository,
    private val stories: StoryRepository,
    private val seriesId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<SeriesDetail>>(UiState.Loading)
    val state: StateFlow<UiState<SeriesDetail>> = _state.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val local = stories.localSeries(seriesId)
            if (local != null) {
                // Show the local copy immediately, then refresh it quietly.
                _state.value = UiState.Content(local)
                _saved.value = true
            } else {
                _state.value = UiState.Loading
            }
            try {
                val detail = repo.load(seriesId.toString())
                if (detail.works.isNotEmpty() || local == null) {
                    _state.value = if (detail.works.isEmpty()) UiState.Empty else UiState.Content(detail)
                }
                _saved.value = stories.isLocalSeriesSaved(seriesId)
                if (_saved.value) stories.saveLocalSeries(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (local == null) _state.value = UiState.Error(e.toApiError())
            }
        }
    }

    fun toggleSaved(detail: SeriesDetail) {
        viewModelScope.launch {
            if (_saved.value) {
                stories.removeLocalSeries(seriesId)
                _saved.value = false
            } else {
                stories.saveLocalSeries(detail)
                _saved.value = true
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
        factory = vmFactory { SeriesViewModel(container.series, container.stories, seriesId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        val detail = (state as? UiState.Content<SeriesDetail>)?.data
        ScreenHeader(title = detail?.title ?: "Series", onBack = onBack) {
            val slug = seriesSlug(detail?.url)
            if (detail != null && slug != null) {
                WebButton(
                    WebTarget(
                        url = container.routes.seriesWebUrl(slug),
                        title = detail.title,
                    ),
                )
            }
            if (detail != null) {
                TextButton(onClick = { vm.toggleSaved(detail) }) {
                    Text(if (saved) "Saved" else "Save")
                }
            }
        }
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            UiState.Empty -> EmptyBox("This series has no parts yet.")
            is UiState.Error -> ErrorBox(s.error, onRetry = { vm.load() })
            is UiState.Content -> {
                val series = s.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                ) {
                    item {
                        SeriesInfo(
                            series = series,
                            saved = saved,
                            onToggleSaved = { vm.toggleSaved(series) },
                            onAuthor = onAuthor,
                        )
                    }
                    item {
                        Text(
                            text = "Chapters",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
                        )
                    }
                    itemsIndexed(series.works) { index, story ->
                        StoryCard(
                            story = story,
                            onClick = { onStory(story.id) },
                            leadingLabel = "${index + 1}",
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesInfo(
    series: SeriesDetail,
    saved: Boolean,
    onToggleSaved: () -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val progress = LocalStoryProgress.current
    val percent = series.works.takeIf { it.isNotEmpty() }?.let { works ->
        val total = works.sumOf { story ->
            (progress[story.id]?.percent?.coerceIn(0f, 1f) ?: 0f).toDouble()
        }
        ((total / works.size) * 100.0).roundToInt().coerceIn(0, 100)
    }?.takeIf { it > 0 }
    val completed = percent != null && percent >= 100

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val meta = listOfNotNull(
                        series.authorName,
                        "${series.works.size} chapter${if (series.works.size == 1) "" else "s"}",
                        series.state?.replace('_', ' '),
                    ).joinToString(" · ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (percent != null) {
                    SeriesProgressBadge(percent = percent, completed = completed)
                }
                if (saved) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = onToggleSaved,
                        label = { Text("Saved locally") },
                    )
                }
            }
            val description = series.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            val authorId = series.authorId
            if (authorId != null) {
                TextButton(
                    onClick = { onAuthor(authorId) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text("More by ${series.authorName ?: "this author"}")
                }
            }
        }
    }
}

/**
 * The API can return either a bare slug or a full page URL. The website URL
 * builder only needs the final path segment.
 */
private fun seriesSlug(raw: String?): String? {
    val value = raw
        ?.trim()
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.trimEnd('/')
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return value.substringAfterLast('/').ifBlank { value }
}
