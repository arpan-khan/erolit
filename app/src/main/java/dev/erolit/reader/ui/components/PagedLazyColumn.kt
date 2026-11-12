package dev.erolit.reader.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A LazyColumn that auto-loads the next page when the user scrolls near the end.
 *
 * `onLoadMore` is invoked once per page when the last visible index reaches
 * [items].size - 3, while `isLoadingMore` is false and `!endReached`.
 */
@Composable
fun <T : Any> PagedLazyColumn(
    items: List<T>,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Int, T) -> Unit,
) {
    val state = rememberLazyListState()
    val loadTriggeredRef = remember { mutableStateOf(false) }

    val listState = state
    LaunchedEffect(listState, items.size, isLoadingMore, endReached) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val lastIndex = visible.lastOrNull()?.index?.minus(1) ?: -1
        val threshold = items.size - 3
        if (
            lastIndex >= threshold &&
            !isLoadingMore &&
            !endReached &&
            !loadTriggeredRef.value
        ) {
            loadTriggeredRef.value = true
            launch { onLoadMore() }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        itemsIndexed(items, key = { _, item -> item.toString() }) { index, item ->
            Box(modifier = Modifier.animateItem()) {
                itemContent(index, item)
            }
        }
        if (isLoadingMore && !endReached) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                )
            }
        }
    }
}
