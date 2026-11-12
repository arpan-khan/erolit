package dev.erolit.reader.ui.lists

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.auth.AuthState
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.data.repo.ListsRepository
import dev.erolit.reader.domain.AuthorProfile
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.domain.UserList
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.SectionHeader
import dev.erolit.reader.ui.components.SeriesCard
import dev.erolit.reader.ui.components.StoryAction
import dev.erolit.reader.ui.components.StoryCard
import dev.erolit.reader.ui.login.LoginForm
import dev.erolit.reader.ui.settings.SwitchRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyListsViewModel(private val repo: ListsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<UserList>>>(UiState.Loading)
    val state: StateFlow<UiState<List<UserList>>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val lists = repo.myLists()
                _state.value = if (lists.isEmpty()) UiState.Empty else UiState.Content(lists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Error(e.toApiError())
            }
        }
    }

    fun create(userId: Long, title: String, isPrivate: Boolean) {
        viewModelScope.launch {
            _message.value = try {
                repo.createList(userId, title.trim(), isPrivate)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toApiError().userMessage()
            }
            load()
        }
    }

    fun delete(list: UserList) {
        viewModelScope.launch {
            _message.value = try {
                repo.deleteList(list.id)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toApiError().userMessage()
            }
            load()
        }
    }
}

@Composable
fun MyListsScreen(
    container: AppContainer,
    onList: (String) -> Unit,
    onStory: (Long) -> Unit,
    onSeries: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "My Lists", onBack = null)
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Local") },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("History") },
            )
            Tab(
                selected = tab == 2,
                onClick = { tab = 2 },
                text = { Text("Account") },
            )
        }
        Crossfade(
            targetState = tab,
            animationSpec = tween(durationMillis = 180),
            label = "my-lists-tab",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { selected ->
            when (selected) {
                0 -> LocalList(container, onStory, onSeries, onAuthor)
                1 -> HistoryList(container, onStory)
                else -> AccountLists(container, onList)
            }
        }
    }
}

@Composable
private fun LocalList(
    container: AppContainer,
    onStory: (Long) -> Unit,
    onSeries: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val storiesFlow = remember { container.stories.observeLocalSaved() }
    val seriesFlow = remember { container.stories.observeLocalSeries() }
    val authorsFlow = remember { container.authors.observeLocalAuthors() }
    val stories by storiesFlow.collectAsStateWithLifecycle(initialValue = emptyList<StorySummary>())
    val series by seriesFlow.collectAsStateWithLifecycle(initialValue = emptyList<SeriesSummary>())
    val authors by authorsFlow.collectAsStateWithLifecycle(initialValue = emptyList<AuthorProfile>())
    val scope = rememberCoroutineScope()

    if (stories.isEmpty() && series.isEmpty() && authors.isEmpty()) {
        EmptyBox(
            "Nothing saved for local reading yet. Long-press a story and choose " +
                "\"Save for local reading\", open a series and tap Save, or save an author " +
                "from their profile.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        ) {
            if (series.isNotEmpty()) {
                item(key = "header-series") { SectionHeader("Saved series") }
                itemsIndexed(series, key = { _, item -> "series-${item.id}" }) { _, item ->
                    SeriesCard(
                        series = item,
                        onClick = { onSeries(item.id) },
                        modifier = Modifier.animateItem(),
                        saved = true,
                        extraActions = listOf(
                            StoryAction(
                                label = "Remove from local list",
                                supporting = "Stops showing this series under Saved series",
                                onClick = { scope.launch { container.stories.removeLocalSeries(item.id) } },
                            ),
                        ),
                    )
                }
            }
            if (stories.isNotEmpty()) {
                item(key = "header-stories") { SectionHeader("Saved stories") }
                itemsIndexed(stories, key = { _, item -> "story-${item.id}" }) { _, story ->
                    StoryCard(
                        story = story,
                        onClick = { onStory(story.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (authors.isNotEmpty()) {
                item(key = "header-authors") { SectionHeader("Saved authors") }
                itemsIndexed(authors, key = { _, author -> "author-${author.id}" }) { _, author ->
                    AuthorCard(
                        author = author,
                        onClick = { onAuthor(author.id) },
                        onRemove = { scope.launch { container.authors.removeLocalAuthor(author.id) } },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    container: AppContainer,
    onStory: (Long) -> Unit,
) {
    val historyFlow = remember { container.stories.observeHistory() }
    val history by historyFlow.collectAsStateWithLifecycle(
        initialValue = emptyList<dev.erolit.reader.domain.HistoryEntry>(),
    )

    if (history.isEmpty()) {
        EmptyBox("No reading history yet. Stories you open will appear here.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 16.dp),
        ) {
            itemsIndexed(history, key = { _, entry -> "history-${entry.story.id}" }) { _, entry ->
                StoryCard(
                    story = entry.story,
                    onClick = { onStory(entry.story.id) },
                    leadingMeta = remember(entry.openedAt) { formatOpenedAt(entry.openedAt) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun AuthorCard(
    author: AuthorProfile,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(
                onClickLabel = "Open author ${author.username}",
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = author.username.trim().firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author.username,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(
                    author.storiesCount?.let { "$it stories" },
                    author.seriesCount?.let { "$it series" },
                    author.followers?.let { "$it followers" },
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val bio = author.bio
                if (!bio.isNullOrBlank()) {
                    Text(
                        text = bio.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

private fun formatOpenedAt(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun AccountLists(container: AppContainer, onList: (String) -> Unit) {
    val auth by container.session.state.collectAsStateWithLifecycle()
    when (val a = auth) {
        AuthState.Loading -> LoadingBox()
        AuthState.Anonymous -> LoginForm(container)
        AuthState.Expired -> LoginForm(container, notice = "Your session expired. Please sign in again.")
        is AuthState.SignedIn -> SignedInLists(container, a.username, a.userId, onList)
    }
}

@Composable
private fun SignedInLists(
    container: AppContainer,
    username: String,
    userId: Long,
    onList: (String) -> Unit,
) {
    val vm: MyListsViewModel = viewModel(
        key = "lists-$userId",
        factory = vmFactory { MyListsViewModel(container.lists) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<UserList?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Signed in as $username",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showCreate = true }) { Text("New list") }
            TextButton(onClick = { scope.launch { container.session.logout() } }) { Text("Sign out") }
        }
        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            UiState.Empty -> EmptyBox("You have no lists yet.")
            is UiState.Error -> ErrorBox(s.error, onRetry = { vm.load() })
            is UiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(s.data) { _, list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClickLabel = "Open list ${list.title}",
                                role = Role.Button,
                                onClick = { onList(list.urlname) },
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(list.title, style = MaterialTheme.typography.titleMedium)
                            val meta = listOfNotNull(
                                "${list.storiesCount} stories",
                                if (list.isPrivate) "private" else null,
                            ).joinToString(" · ")
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (list.isDeletable) {
                            TextButton(onClick = { toDelete = list }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var title by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New list") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    SwitchRow("Private", isPrivate) { isPrivate = it }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        vm.create(userId, title, isPrivate)
                        showCreate = false
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete list?") },
            text = { Text("\"${pending.title}\" will be deleted. The stories themselves are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(pending)
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
