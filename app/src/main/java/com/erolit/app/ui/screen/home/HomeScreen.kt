package com.erolit.app.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.erolit.app.domain.model.AllCategories
import com.erolit.app.domain.model.Story
import com.erolit.app.ui.components.ErrorView
import com.erolit.app.ui.components.FullScreenLoader
import com.erolit.app.ui.components.SectionHeader
import com.erolit.app.ui.components.StoryCard
import com.erolit.app.ui.navigation.Screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "EroLit", 
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications, 
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoadingNew && uiState.newStories.isEmpty() -> FullScreenLoader(modifier = Modifier.padding(padding))
            uiState.error != null && uiState.newStories.isEmpty() -> ErrorView(
                message = uiState.error ?: "Something went wrong",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // ── Continue Reading ──────────────────────
                    if (uiState.recentlyRead.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Continue Reading", actionLabel = "History", onAction = { navController.navigate(Screen.Library.route) })
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.recentlyRead.take(5), key = { "continue_${it.slug}" }) { story ->
                                    ContinueReadingCard(
                                        story = story,
                                        onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── Browse by Genre ─────────────────────────────────────
                    item {
                        SectionHeader(title = "Browse by Genre")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(AllCategories.take(6), key = { "genre_${it.slug}" }) { cat ->
                                GenrePill(
                                    categoryName = cat.name,
                                    onClick = { navController.navigate(Screen.CategoryDetail.createRoute(cat.slug)) }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Top Rated ─────────────────────────────────────────────
                    if (uiState.topRated.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Top Rated")
                        }
                        items(uiState.topRated.take(8), key = { "top_${it.slug}" }) { story ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                TopRatedCard(
                                    story = story,
                                    onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    // ── New Arrivals ─────────────────────────────────────────────
                    if (uiState.newStories.isNotEmpty()) {
                        item {
                            SectionHeader(title = "New Arrivals")
                        }
                        itemsIndexed(uiState.newStories.take(10), key = { _, it -> "arrival_${it.slug}" }) { index, story ->
                            NewArrivalItem(
                                index = index + 1,
                                story = story,
                                onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueReadingCard(story: Story, onClick: () -> Unit) {
    // Generate a pseudo-random gradient for mockup purposes
    val colorStart = com.erolit.app.ui.theme.PrimaryMidnight
    val colorEnd = com.erolit.app.ui.theme.PrimaryDark
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colorStart, colorEnd))
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = story.author.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(story.readingProgress * 100).toInt()}% read",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun GenrePill(categoryName: String, onClick: () -> Unit) {
    // Simple colored pill based on name
    val color = when(categoryName.lowercase()) {
        "romantic" -> com.erolit.app.ui.theme.GenreRomantic
        "historical" -> com.erolit.app.ui.theme.GenreHistorical
        "contemporary" -> com.erolit.app.ui.theme.GenreContemporary
        "classic" -> com.erolit.app.ui.theme.GenreClassic
        "poetry" -> com.erolit.app.ui.theme.GenrePoetry
        else -> com.erolit.app.ui.theme.PrimaryMidnight
    }
    
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelLarge,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
fun TopRatedCard(story: Story, onClick: () -> Unit) {
    val colorStart = com.erolit.app.ui.theme.DarkSurface
    val colorEnd = com.erolit.app.ui.theme.PrimaryDark.copy(alpha = 0.2f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colorStart, colorEnd))
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = story.author.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allTags = mutableListOf<String>()
                    if (story.category.name.isNotBlank()) allTags.add(story.category.name)
                    allTags.addAll(story.tags.take(2))
                    
                    allTags.forEach { tag ->
                        Text(
                            text = tag.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewArrivalItem(index: Int, story: Story, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = story.author.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
