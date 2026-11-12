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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.erolit.reader.R
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.ui.common.FeedState
import dev.erolit.reader.ui.common.LocalOpenWeb
import dev.erolit.reader.ui.common.WebTarget

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(error: ApiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    StateMessage(
        icon = Icons.Default.Warning,
        title = "Something went wrong",
        message = error.userMessage(),
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier,
    ) {
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    StateMessage(
        icon = Icons.Default.Info,
        title = "Nothing here yet",
        message = text,
        modifier = modifier,
    )
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(64.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
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
                    .padding(start = if (onBack == null) 16.dp else 4.dp, end = 8.dp),
            )
            actions()
        }
    }
}

/**
 * A globe button that opens the matching page of the upstream website in the
 * in-app WebView. Place it in a [ScreenHeader] action slot.
 */
@Composable
fun WebButton(target: WebTarget, modifier: Modifier = Modifier) {
    val openWeb = LocalOpenWeb.current
    IconButton(onClick = { openWeb(target) }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_globe),
            contentDescription = "Open on the website",
        )
    }
}

/**
 * The same action as a filled button for warning cards, where the user needs
 * an obvious way to leave the app's limited view.
 */
@Composable
fun WebActionButton(
    target: WebTarget,
    label: String = "Open website",
    modifier: Modifier = Modifier,
) {
    val openWeb = LocalOpenWeb.current
    Button(onClick = { openWeb(target) }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_globe),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 6.dp),
    )
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
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val err = state.error
    when {
        !state.started || (state.loading && state.items.isEmpty()) -> Column(modifier.fillMaxSize()) {
            header?.invoke()
            LoadingBox(Modifier.weight(1f))
        }
        err != null && state.items.isEmpty() -> Column(modifier.fillMaxSize()) {
            header?.invoke()
            ErrorBox(err, onRetry, Modifier.weight(1f))
        }
        state.items.isEmpty() -> Column(modifier.fillMaxSize()) {
            header?.invoke()
            EmptyBox(emptyText, Modifier.weight(1f))
        }
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
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                if (header != null) {
                    item(key = "feed-header") { header() }
                }
                itemsIndexed(state.items) { _, item ->
                    Box(modifier = Modifier.animateItem()) {
                        itemContent(item)
                    }
                }
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
                            TextButton(onClick = onRetry) { Text("Try again") }
                        }
                    }
                }
            }
        }
    }
}
