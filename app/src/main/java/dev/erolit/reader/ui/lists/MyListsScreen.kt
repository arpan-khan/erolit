package dev.erolit.reader.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.erolit.reader.domain.UserList
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.StoryCard
import dev.erolit.reader.ui.login.LoginForm
import dev.erolit.reader.ui.settings.SwitchRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "My Lists", onBack = null)
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Offline") },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Account") },
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (tab == 0) {
                OfflineList(container, onStory)
            } else {
                AccountLists(container, onList)
            }
        }
    }
}

@Composable
private fun OfflineList(container: AppContainer, onStory: (Long) -> Unit) {
    val flow = remember { container.stories.observeLocalSaved() }
    val saved by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    if (saved.isEmpty()) {
        EmptyBox("No offline entries yet. Open a story, tap Save, then choose \"Save to offline list\".")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(saved) { _, story ->
                Column {
                    StoryCard(
                        story = story,
                        onClick = { onStory(story.id) },
                    )
                    TextButton(
                        onClick = { scope.launch { container.stories.removeLocal(story.id) } },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    ) { Text("Remove from offline list") }
                }
            }
        }
    }
}

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
