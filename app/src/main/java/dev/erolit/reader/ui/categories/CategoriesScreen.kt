package dev.erolit.reader.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.domain.Category
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.components.EmptyBox
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import kotlinx.coroutines.CancellationException

@Composable
fun CategoriesScreen(container: AppContainer, onCategory: (Int, String) -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<UiState<List<Category>>>(UiState.Loading) }

    LaunchedEffect(attempt) {
        state = UiState.Loading
        state = try {
            val all = container.catalog.loadCategories().filter { it.type == null || it.type == "story" }
            if (all.isEmpty()) UiState.Empty else UiState.Content(all)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            UiState.Error(e.toApiError())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Categories", onBack = null)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            UiState.Empty -> EmptyBox("No categories available.")
            is UiState.Error -> ErrorBox(s.error, onRetry = { attempt += 1 })
            is UiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(s.data) { _, category ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClickLabel = "Open category ${category.name}",
                                role = Role.Button,
                                onClick = { onCategory(category.id, category.name) },
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                        category.submissionCount?.let {
                            Text(
                                text = "$it stories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
