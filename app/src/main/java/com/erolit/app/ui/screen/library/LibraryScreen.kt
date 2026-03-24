package com.erolit.app.ui.screen.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.erolit.app.ui.components.FullScreenLoader
import com.erolit.app.ui.components.StoryCard
import com.erolit.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    val tabs = listOf("Saved", "Downloads", "Recent", "Lists") // Reordered to match mockup

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedTab == 3) {
                        IconButton(onClick = { showNewListDialog = true }) {
                            Icon(Icons.Filled.Add, "New list")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            FullScreenLoader(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { 
                            Text(
                                title, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedTab == idx) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                // ── Saved ──────────────────────────────────────────────────
                0 -> StoryListTab(
                    stories = state.savedStories,
                    emptyMessage = "No saved stories.",
                    navController = navController
                )
                // ── Downloads ──────────────────────────────────────────────
                1 -> StoryListTab(
                    stories = state.downloadedStories,
                    emptyMessage = "No downloaded stories.",
                    navController = navController,
                    onDelete = viewModel::deleteDownload
                )
                // ── Recent ─────────────────────────────────────────────────
                2 -> StoryListTab(
                    stories = state.recentlyRead,
                    emptyMessage = "No reading history.",
                    navController = navController
                )
                // ── Lists ──────────────────────────────────────────────────
                3 -> {
                    if (state.readingLists.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create your first\nreading list.",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.readingLists, key = { it.id }) { list ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(list.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                            if (list.description.isNotBlank()) {
                                                Text(list.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        TextButton(onClick = { viewModel.deleteList(list.id) }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New reading list dialog
    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New Reading List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            viewModel.createReadingList(newListName.trim())
                            newListName = ""
                            showNewListDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StoryListTab(
    stories: List<com.erolit.app.domain.model.Story>,
    emptyMessage: String,
    navController: NavController,
    onDelete: ((String) -> Unit)? = null
) {
    if (stories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(stories, key = { it.slug }) { story ->
                Box {
                    StoryCard(
                        story = story,
                        onClick = { navController.navigate(Screen.Reader.createRoute(story.slug)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    if (onDelete != null) {
                        IconButton(
                            onClick = { onDelete(story.slug) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.8f), shape=CircleShape)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
