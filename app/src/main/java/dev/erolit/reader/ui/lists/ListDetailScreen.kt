package dev.erolit.reader.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.data.repo.ListsRepository
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.domain.UserList
import dev.erolit.reader.ui.common.FeedPage
import dev.erolit.reader.ui.common.PagedFeed
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.FeedList
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.StoryAction
import dev.erolit.reader.ui.components.StoryCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListDetailViewModel(
    private val repo: ListsRepository,
    private val urlname: String,
) : ViewModel() {
    val feed = PagedFeed<StorySummary>(viewModelScope)

    private val _list = MutableStateFlow<UserList?>(null)
    val list: StateFlow<UserList?> = _list.asStateFlow()

    fun ensureStarted() {
        if (feed.isStarted) return
        feed.start { page ->
            val r = repo.listWorks(urlname, page)
            _list.value = r.list
            FeedPage(r.items, page < r.lastPage)
        }
    }

    fun remove(story: StorySummary) {
        val listId = _list.value?.id ?: return
        viewModelScope.launch {
            try {
                repo.removeStory(story.id, listId)
                feed.removeWhere { it.id == story.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // leave the item in place; the user can retry
            }
        }
    }
}

@Composable
fun ListDetailScreen(
    container: AppContainer,
    urlname: String,
    onBack: () -> Unit,
    onStory: (Long) -> Unit,
) {
    val vm: ListDetailViewModel = viewModel(
        key = "list-$urlname",
        factory = vmFactory { ListDetailViewModel(container.lists, urlname) },
    )
    LaunchedEffect(urlname) { vm.ensureStarted() }
    val list by vm.list.collectAsStateWithLifecycle()
    val state by vm.feed.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = list?.title ?: "List", onBack = onBack)
        FeedList(
            state = state,
            onLoadMore = { vm.feed.loadMore() },
            onRetry = { vm.feed.retry() },
            emptyText = "This list is empty.",
        ) { story ->
            StoryCard(
                story = story,
                onClick = { onStory(story.id) },
                extraActions = listOf(
                    StoryAction(
                        label = "Remove from this list",
                        supporting = list?.title,
                        onClick = { vm.remove(story) },
                    ),
                ),
            )
        }
    }
}
