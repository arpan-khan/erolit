package com.erolit.app.ui.screen.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.erolit.app.domain.model.Story
import com.erolit.app.ui.components.ErrorView
import com.erolit.app.ui.components.FullScreenLoader
import com.erolit.app.ui.navigation.Screen
import com.erolit.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    slug: String,
    navController: NavController,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    LaunchedEffect(slug) { viewModel.init(slug) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    // Multi-select state for pages
    val selectedPages = remember { mutableStateListOf<Int>() }
    val isMultiSelectActive = selectedPages.isNotEmpty()

    // Track if this is the initial launch for this story to avoid resetting scroll on resume
    var isInitialLoad by remember(slug) { mutableStateOf(true) }

    // Scroll to top when page changes or entering reading mode (BUT not on initial resume)
    LaunchedEffect(state.currentPage) {
        if (state.isReadingMode && state.pages.isNotEmpty()) {
            if (!isInitialLoad) {
                listState.scrollToItem(0)
            }
        }
    }

    // Restore scroll position when pages are loaded and we are in reading mode
    LaunchedEffect(state.isReadingMode, state.pages.isNotEmpty()) {
        if (state.isReadingMode && state.pages.isNotEmpty() && isInitialLoad) {
            if (state.scrollFraction > 0.01f) {
                var attempts = 0
                while (attempts < 10) {
                    val info = listState.layoutInfo
                    val totalItems = info.totalItemsCount
                    if (totalItems > 2) {
                        val targetIndex = (totalItems * state.scrollFraction).toInt().coerceIn(0, totalItems - 1)
                        listState.scrollToItem(targetIndex)
                        isInitialLoad = false
                        break
                    }
                    attempts++
                    kotlinx.coroutines.delay(100)
                }
            } else {
                isInitialLoad = false
            }
        }
    }

    // Removed estimatedPage logic that was causing scrolls to reset during fast fling gestures.
    // Progress is now tracked purely by scrollFraction within the current loaded page.


    // Reader theme colors
    val (bg, surface, textColor) = when (state.readerTheme) {
        ReaderTheme.SEPIA -> Triple(SepiaBackground, SepiaSurface, SepiaText)
        ReaderTheme.AMOLED -> Triple(AmoledBackground, AmoledSurface, OnDark)
        ReaderTheme.LIGHT -> Triple(LightBackground, LightSurface, OnLight)
        ReaderTheme.DARK -> Triple(DarkBackground, DarkSurface, OnDark)
    }

    // Track scroll fraction for bookmark
    val scrollFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0 || !state.isReadingMode) 0f
            else listState.firstVisibleItemIndex.toFloat() / info.totalItemsCount
        }
    }
    LaunchedEffect(scrollFraction) {
        if (state.isReadingMode && scrollFraction > 0f) {
            viewModel.onScrollFractionChanged(scrollFraction, slug)
            
            // Auto-mark page as read when reaching the bottom (90%)
            if (scrollFraction > 0.9f) {
                state.pages.firstOrNull()?.pageNumber?.let { viewModel.markPageAsRead(it) }
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = state.isReadingMode) {
        viewModel.exitReadingMode()
    }

Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isMultiSelectActive) "${selectedPages.size} selected"
                                   else if (state.isReadingMode) (state.story?.title ?: "Reading…") 
                                   else "Story Info",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (isMultiSelectActive) selectedPages.clear()
                            else if (state.isReadingMode) viewModel.exitReadingMode() 
                            else navController.popBackStack() 
                        }) {
                            Icon(
                                if (isMultiSelectActive) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Close"
                            )
                        }
                    },
                    actions = {
                        if (isMultiSelectActive) {
                            IconButton(onClick = { 
                                selectedPages.forEach { viewModel.markPageAsRead(it) }
                                selectedPages.clear()
                            }) {
                                Icon(Icons.Filled.DoneAll, "Mark Read")
                            }
                            IconButton(onClick = { 
                                selectedPages.forEach { viewModel.markPageAsUnread(it) }
                                selectedPages.clear()
                            }) {
                                Icon(Icons.Filled.History, "Mark Unread")
                            }
                        } else if (state.isReadingMode) {
                            // Offline badge
                            if (state.isOfflineMode) {
                                Icon(Icons.Filled.WifiOff, contentDescription = "Offline", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(4.dp))
                            }
                            // Settings
                            IconButton(onClick = { navController.navigate(Screen.WebView.createRoute(state.story?.url ?: "https://www.literotica.com")) }) {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Public, contentDescription = "Open in Web")
                            }
                            IconButton(onClick = { viewModel.toggleSettingsSheet() }) {
                                Icon(Icons.Filled.TextFields, contentDescription = "Reader settings")
                            }
                        } else {
                            IconButton(onClick = { viewModel.refreshFromNetwork(slug) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                            }
                            IconButton(onClick = { navController.navigate(Screen.WebView.createRoute(state.story?.url ?: "https://www.literotica.com")) }) {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Public, contentDescription = "Open in Web")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (state.isReadingMode) bg.copy(alpha = 0.97f) else MaterialTheme.colorScheme.surface,
                        titleContentColor = if (state.isReadingMode) textColor else MaterialTheme.colorScheme.onSurface
                    )
                )
                // Persistent Progress Bar below TopAppBar
                if (state.isReadingMode && state.totalPages > 0) {
                    val progressValue = remember(state.currentPage, scrollFraction, state.totalPages) {
                        val absoluteProgress = if (state.totalPages > 1) {
                            (state.currentPage - 1 + scrollFraction) / state.totalPages
                        } else {
                            scrollFraction
                        }
                        absoluteProgress.coerceIn(0f, 1f)
                    }
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = PrimaryLight,
                        trackColor = Color.Transparent
                    )
                }
            }
        },
        floatingActionButton = {
            if (!state.isReadingMode && state.story != null && state.story!!.readingProgress > 0) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::startReading,
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    text = { Text("Resume") },
                    containerColor = PrimaryLight,
                    contentColor = Color.White
                )
            }
        },
        containerColor = if (state.isReadingMode) bg else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            state.isLoading && state.story == null -> FullScreenLoader(modifier = Modifier.padding(innerPadding))
            state.error != null && state.pages.isEmpty() -> ErrorView(
                message = state.error ?: "Error loading story",
                onRetry = viewModel::retryLoadPage,
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                val page = state.pages.firstOrNull()
                val paragraphs = remember(page?.content) { 
                    page?.content?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList() 
                }

                androidx.compose.animation.AnimatedContent(
                    targetState = state.isReadingMode,
                    label = "reader_transition"
                ) { isReading ->
                    if (!isReading) {
                        if (state.story != null) {
                            StoryDetailView(
                                story = state.story!!,
                                innerPadding = innerPadding,
                                onReadClick = viewModel::startReading,
                                onPageClick = viewModel::jumpToPage,
                                onDownloadAllClick = { state.story?.let { viewModel.downloadStory(it.slug) } },
                                onDownloadPageClick = { pageNum -> state.story?.let { viewModel.downloadPage(it.slug, pageNum) } },
                                onSaveClick = { state.story?.let { viewModel.toggleSave(it) } },
                                onWebViewClick = { navController.navigate(Screen.WebView.createRoute(state.story?.url ?: "https://www.literotica.com")) },
                                isDownloaded = state.isDownloaded,
                                downloadedPageNumbers = state.downloadedPageNumbers,
                                readPageNumbers = state.readPageNumbers,
                                selectedPages = selectedPages,
                                isMultiSelectActive = isMultiSelectActive,
                                isSaved = state.isSaved
                            )
                        } else {
                            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Story not found")
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .background(bg),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            // Story header (only shown briefly in reading mode)
                            state.story?.let { story ->
                                item {
                                    Text(
                                        text = story.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = textColor,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("by ", style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.6f))
                                        Text(
                                            text = story.author.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PrimaryLight
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = textColor.copy(alpha = 0.15f))
                                }
                            }

                            // Story page paragraphs
                            if (page != null) {
                                // Previous Page button
                                if (page.pageNumber > 1) {
                                    item(key = "prev_btn") {
                                        OutlinedButton(
                                            onClick = { viewModel.jumpToPage(page.pageNumber - 1) },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Text("Load Previous Page (Page ${page.pageNumber - 1})", color = textColor)
                                        }
                                    }
                                }

                                items(paragraphs.size, key = { "p_${page.pageNumber}_$it" }) { idx ->
                                    Text(
                                        text = paragraphs[idx],
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = state.fontSize.sp,
                                            lineHeight = (state.fontSize * state.lineSpacing).sp,
                                            color = textColor
                                        ),
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )
                                }

                                // Next Page button
                                if (page.pageNumber < state.totalPages) {
                                    item(key = "next_btn") {
                                        OutlinedButton(
                                            onClick = { viewModel.jumpToPage(page.pageNumber + 1) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                        ) {
                                            Text("Load Next Page (Page ${page.pageNumber + 1})", color = textColor)
                                        }
                                    }
                                }
                            }

                            if (state.isLoading && state.pages.isNotEmpty()) {
                                item { InlinePageLoader(textColor) }
                            }

                            // Reading Progress Bar at the end of page
                            if (page != null && state.totalPages > 0) {
                                item {
                                    Surface(
                                        color = bg.copy(alpha = 0.95f),
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    "Page ${page.pageNumber} of ${state.totalPages}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = textColor.copy(alpha = 0.7f)
                                                )
                                                if (page.pageNumber == state.totalPages) {
                                                    Text("End of Story", style = MaterialTheme.typography.labelMedium, color = PrimaryLight)
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { page.pageNumber.toFloat() / state.totalPages },
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                color = PrimaryLight,
                                                trackColor = textColor.copy(alpha = 0.1f)
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        if (state.showSettingsSheet && state.isReadingMode) {
            ReaderSettingsSheet(
                fontSize = state.fontSize,
                lineSpacing = state.lineSpacing,
                readerTheme = state.readerTheme,
                onFontSizeChange = viewModel::setFontSize,
                onLineSpacingChange = viewModel::setLineSpacing,
                onThemeChange = viewModel::setReaderTheme,
                onDismiss = { viewModel.toggleSettingsSheet() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryDetailView(
    story: Story,
    innerPadding: PaddingValues,
    onReadClick: () -> Unit,
    onPageClick: (Int) -> Unit,
    onDownloadAllClick: () -> Unit,
    onDownloadPageClick: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onWebViewClick: () -> Unit,
    isDownloaded: Boolean,
    downloadedPageNumbers: Set<Int>,
    readPageNumbers: Set<Int>,
    selectedPages: MutableList<Int>,
    isMultiSelectActive: Boolean,
    isSaved: Boolean
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Redesigned Header (Tachiyomi style without cover)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(story.author.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Update, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${story.datePublished} • Literotica (EN)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (story.author.followerCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.People, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${story.author.followerCount} followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isSaved) "In library" else "Add to library",
                onClick = onSaveClick,
                color = if (isSaved) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionButton(
                icon = Icons.Filled.Star,
                label = "${story.rating}",
                onClick = {},
                color = StarColor
            )
            ActionButton(
                icon = Icons.Filled.DownloadForOffline,
                label = "Download All",
                onClick = onDownloadAllClick,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionButton(
                icon = Icons.Outlined.Public,
                label = "WebView",
                onClick = onWebViewClick,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Description Section
        Column(modifier = Modifier.padding(horizontal = 16.dp).clickable { isDescriptionExpanded = !isDescriptionExpanded }) {
            Text(
                text = story.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    if (isDescriptionExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Genres/Tags
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GenreChip(story.category.name)
            story.tags.forEach { tag -> GenreChip(tag) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Chapter/Page List Header
        Text(
            text = "${story.pageCount} pages",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )

        // Page List
        for (i in 1..story.pageCount) {
             val isPageRead = readPageNumbers.contains(i)
             val isPageSelected = selectedPages.contains(i)
             PageListItem(
                 pageNumber = i,
                 datePublished = story.datePublished,
                 isRead = isPageRead,
                 isSelected = isPageSelected,
                 onClick = { 
                     if (isMultiSelectActive) {
                         if (isPageSelected) selectedPages.remove(i) else selectedPages.add(i)
                     } else {
                         onPageClick(i) 
                     }
                 },
                 onLongClick = {
                     if (!isMultiSelectActive) selectedPages.add(i)
                 },
                 onDownloadClick = { onDownloadPageClick(i) },
                 isDownloaded = downloadedPageNumbers.contains(i)
             )
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun GenreChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PageListItem(
    pageNumber: Int,
    datePublished: String,
    isRead: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
    isDownloaded: Boolean
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                          else if (isRead) Color.Transparent
                          else Color.Transparent
    
    val alpha = if (isRead && !isSelected) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isRead) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else PrimaryLight.copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Page $pageNumber", 
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$datePublished • www.literotica.com", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onDownloadClick) {
                if (isDownloaded) {
                    Icon(Icons.Filled.FileDownloadDone, null, modifier = Modifier.size(20.dp), tint = PrimaryLight)
                } else {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InlinePageLoader(textColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColor.copy(alpha = 0.5f))
    }
}
