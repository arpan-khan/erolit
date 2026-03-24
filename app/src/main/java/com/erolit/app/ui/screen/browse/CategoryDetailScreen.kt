package com.erolit.app.ui.screen.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.erolit.app.domain.model.AllCategories
import com.erolit.app.domain.repository.StoryRepository
import com.erolit.app.ui.components.ErrorView
import com.erolit.app.ui.components.FullScreenLoader
import com.erolit.app.ui.components.InlineLoader
import com.erolit.app.ui.components.StoryCard
import com.erolit.app.ui.navigation.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.erolit.app.domain.model.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {
    fun getStories(slug: String): Flow<PagingData<Story>> =
        storyRepository.getCategoryStories(slug).cachedIn(viewModelScope)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categorySlug: String,
    navController: NavController,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val categoryName = AllCategories.find { it.slug == categorySlug }?.name ?: categorySlug
    val stories = viewModel.getStories(categorySlug).collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (stories.loadState.refresh) {
            is LoadState.Loading -> FullScreenLoader(modifier = Modifier.padding(padding))
            is LoadState.Error -> ErrorView(
                message = (stories.loadState.refresh as LoadState.Error).error.message ?: "Error",
                onRetry = { stories.retry() },
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(stories.itemCount) { index ->
                        stories[index]?.let { story ->
                            StoryCard(
                                story = story,
                                onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                    when (stories.loadState.append) {
                        is LoadState.Loading -> item { InlineLoader() }
                        is LoadState.Error -> item {
                            TextButton(
                                onClick = { stories.retry() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Retry loading more") }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
