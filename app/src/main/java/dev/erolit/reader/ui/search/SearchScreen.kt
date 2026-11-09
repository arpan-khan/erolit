package dev.erolit.reader.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.FeedList
import dev.erolit.reader.ui.components.StoryCard

@Composable
fun SearchScreen(
    container: AppContainer,
    onStory: (Long) -> Unit,
    onAuthor: (Long) -> Unit,
) {
    val vm: SearchViewModel = viewModel(factory = vmFactory { SearchViewModel(container.search) })
    val query by vm.query.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val submitted by vm.submitted.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.run() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.run() }, enabled = query.isNotBlank()) { Text("Go") }
        }
        ChipRow {
            Chip("Stories", type == "stories") { vm.setType("stories") }
            Chip("Poetry", type == "poems") { vm.setType("poems") }
            Chip("Members", type == "members") { vm.setType("members") }
        }
        if (type != "members") {
            ChipRow {
                Chip("Relevance", sort == null) { vm.setSort(null) }
                Chip("Top rated", sort == "rating desc") { vm.setSort("rating desc") }
                Chip("Newest", sort == "date desc") { vm.setSort("date desc") }
            }
        }
        if (!submitted) {
            if (history.isEmpty()) {
                EmptyBox("Type a word or phrase and tap Go.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.clearHistory() }) { Text("Clear") }
                }
                ChipRow {
                    history.take(10).forEach { item -> Chip(item, false) { vm.useHistory(item) } }
                }
            }
        } else if (type == "members") {
            val state by vm.members.state.collectAsStateWithLifecycle()
            FeedList(
                state = state,
                onLoadMore = { vm.members.loadMore() },
                onRetry = { vm.members.retry() },
                emptyText = "No members found.",
            ) { member ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Open author ${member.username}",
                            role = Role.Button,
                            onClick = { onAuthor(member.id) },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(member.username, style = MaterialTheme.typography.titleMedium)
                    val bio = member.bio
                    if (!bio.isNullOrBlank()) {
                        Text(
                            text = bio.take(140),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        } else {
            val state by vm.stories.state.collectAsStateWithLifecycle()
            FeedList(
                state = state,
                onLoadMore = { vm.stories.loadMore() },
                onRetry = { vm.stories.retry() },
                emptyText = "Nothing found. Try another word.",
            ) { story ->
                StoryCard(
                    story = story,
                    onClick = { onStory(story.id) },
                    onAuthor = story.authorId?.let { id -> { onAuthor(id) } },
                )
            }
        }
    }
}
