package dev.erolit.reader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.common.FeedState
import dev.erolit.reader.ui.common.LocalStoryProgress
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StoryCard(
    story: StorySummary,
    onClick: () -> Unit,
    onAuthor: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val progress = LocalStoryProgress.current[story.id]
    val percent = ((progress?.percent ?: 0f).coerceIn(0f, 1f) * 100f).roundToInt()
    val completed = percent >= 100
    val textAlpha = if (completed) 0.55f else 1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${story.title}", role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (completed) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        val byline = listOfNotNull(story.authorName, story.categoryName, story.publishedAt)
            .joinToString(" · ")
        if (byline.isNotEmpty()) {
            Text(
                text = byline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val description = story.description
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        val meta = listOfNotNull(
            story.rating?.let { "★ " + String.format(Locale.US, "%.2f", it) },
            story.words?.let { "$it words" },
            story.readingMinutes?.let { "$it min" },
            if (story.isHot) "Hot" else null,
            if (story.isNew) "New" else null,
            story.seriesTitle?.let { "Series: $it" },
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (onAuthor != null && story.authorId != null) {
            TextButton(
                onClick = onAuthor,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
                ),
            ) {
                Text("More by ${story.authorName ?: "this author"}")
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(error: ApiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error.userMessage(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onBack == null) 12.dp else 4.dp, end = 8.dp),
            )
            actions()
        }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun <T> FeedList(
    state: FeedState<T>,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val err = state.error
    when {
        !state.started || (state.loading && state.items.isEmpty()) -> LoadingBox(modifier)
        err != null && state.items.isEmpty() -> ErrorBox(err, onRetry, modifier)
        state.items.isEmpty() -> EmptyBox(emptyText, modifier)
        else -> {
            val listState = rememberLazyListState()
            val nearEnd by remember {
                derivedStateOf {
                    val total = listState.layoutInfo.totalItemsCount
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    total > 0 && last >= total - 4
                }
            }
            LaunchedEffect(nearEnd, state.items.size, state.loadingMore, state.endReached, err) {
                if (nearEnd && !state.loadingMore && !state.endReached && err == null) onLoadMore()
            }
            LazyColumn(modifier = modifier.fillMaxSize(), state = listState) {
                itemsIndexed(state.items) { _, item -> itemContent(item) }
                if (state.loadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
                if (err != null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = err.userMessage(),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Button(onClick = onRetry) { Text("Retry") }
                        }
                    }
                }
            }
        }
    }
}
