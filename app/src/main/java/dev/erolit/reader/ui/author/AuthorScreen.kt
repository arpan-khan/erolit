package dev.erolit.reader.ui.author

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.data.repo.AuthorRepository
import dev.erolit.reader.domain.AuthorProfile
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.common.FeedPage
import dev.erolit.reader.ui.common.PagedFeed
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.FeedList
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.StoryCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthorViewModel(
    private val repo: AuthorRepository,
    private val authorId: Long,
) : ViewModel() {
    val works = PagedFeed<StorySummary>(viewModelScope)

    private val _profile = MutableStateFlow<AuthorProfile?>(null)
    val profile: StateFlow<AuthorProfile?> = _profile.asStateFlow()

    fun ensureStarted() {
        if (works.isStarted) return
        works.start { page ->
            val r = repo.works(authorId, page)
            FeedPage(r.items, r.items.isNotEmpty() && page * r.pageSize < r.total)
        }
        viewModelScope.launch {
            try {
                _profile.value = repo.profile(authorId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                // The works list still works without the profile header.
            }
        }
    }
}

@Composable
fun AuthorScreen(
    container: AppContainer,
    authorId: Long,
    onBack: () -> Unit,
    onStory: (Long) -> Unit,
) {
    val vm: AuthorViewModel = viewModel(
        key = "author-$authorId",
        factory = vmFactory { AuthorViewModel(container.authors, authorId) },
    )
    LaunchedEffect(authorId) { vm.ensureStarted() }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val state by vm.works.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = profile?.username ?: "Author", onBack = onBack)
        val p = profile
        if (p != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                val meta = listOfNotNull(
                    p.storiesCount?.let { "$it stories" },
                    p.seriesCount?.let { "$it series" },
                    p.followers?.let { "$it followers" },
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val bio = p.bio
                if (!bio.isNullOrBlank()) {
                    Text(
                        text = bio.trim().take(240),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        FeedList(
            state = state,
            onLoadMore = { vm.works.loadMore() },
            onRetry = { vm.works.retry() },
            emptyText = "No stories by this author.",
        ) { story ->
            StoryCard(story = story, onClick = { onStory(story.id) })
        }
    }
}
