package com.erolit.app.ui.screen.author

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.erolit.app.ui.components.ErrorView
import com.erolit.app.ui.components.FullScreenLoader
import com.erolit.app.ui.components.StoryCard
import com.erolit.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    username: String,
    navController: NavController,
    viewModel: AuthorViewModel = hiltViewModel()
) {
    LaunchedEffect(username) { viewModel.load(username) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.author?.displayName ?: username) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.author == null -> FullScreenLoader(modifier = Modifier.padding(padding))
            state.error != null && state.author == null -> ErrorView(
                message = state.error ?: "Error",
                onRetry = { viewModel.load(username) },
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Author header
                state.author?.let { author ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (author.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = author.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(96.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(author.displayName, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatChip("${author.storyCount}", "Stories")
                                StatChip("${author.followerCount}", "Followers")
                            }
                            if (author.bio.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = author.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                        }
                    }

                    // Stories by author
                    item {
                        Text(
                            "Stories by ${author.displayName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                items(state.stories, key = { it.slug }) { story ->
                    StoryCard(
                        story = story,
                        onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
