package dev.erolit.reader.ui.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.auth.AuthState
import dev.erolit.reader.core.net.toApiError
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.domain.UserList
import dev.erolit.reader.ui.common.vmFactory
import dev.erolit.reader.ui.common.WebTarget
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.components.WebButton
import dev.erolit.reader.ui.settings.ReadingSettingsPanel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun ScrollState.fraction(endPaddingPx: Float): Float = when {
    maxValue in 1 until Int.MAX_VALUE -> {
        // The reader keeps a little padding below the last paragraph so the
        // text does not sit flush against the progress bar. Count that padding
        // as part of the page: when the last line reaches the bottom of the
        // viewport the reader has finished the page even though the scroll
        // container can still move a few pixels further.
        val effectiveMax = (maxValue - endPaddingPx).coerceAtLeast(1f)
        (value / effectiveMax).coerceIn(0f, 1f)
    }
    // A page shorter than the screen cannot scroll. Once actually laid out,
    // that means the whole page is visible, so it counts as fully read.
    maxValue == 0 && viewportSize > 0 -> 1f
    else -> 0f
}

private fun splitParagraphs(raw: String): List<String> {
    val text = raw.replace("\r\n", "\n").replace('\r', '\n')
    var parts = text.split(Regex("\n{2,}"))
    if (parts.size <= 1) parts = text.split("\n")
    return parts
        .map {
            it.replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .trim()
        }
        .filter { it.isNotEmpty() }
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(container: AppContainer, storyId: Long, onBack: () -> Unit) {
    val vm: ReaderViewModel = viewModel(
        key = "reader-$storyId",
        factory = vmFactory { ReaderViewModel(container.stories, storyId, container.appScope) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scroll = rememberScrollState()
    val endPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }
    val screenActive = remember { mutableStateOf(true) }
    // Last fraction observed while the reader was actually visible. Saving
    // this instead of recomputing on dispose prevents the viewport change
    // caused by the returning bottom bar from lowering the saved percentage.
    val latestFraction = remember { mutableStateOf(0f) }
    val reachedEnd = remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(state.page, state.text) {
        if (state.text.isEmpty()) return@LaunchedEffect
        // Wait until the page has been measured. A non-scrollable page keeps
        // maxValue at 0 forever, so waiting for maxValue > 0 would hang.
        snapshotFlow { scroll.viewportSize to scroll.maxValue }
            .first { (viewport, max) -> viewport > 0 && max != Int.MAX_VALUE }
        if (state.restoreFraction > 0f && scroll.maxValue > 0) {
            scroll.scrollTo((state.restoreFraction * scroll.maxValue).toInt())
            latestFraction.value = state.restoreFraction
            if (state.page >= state.pagesCount && state.restoreFraction >= 0.999f) {
                reachedEnd.value = true
                vm.markCompleted()
            }
        } else {
            scroll.scrollTo(0)
            latestFraction.value = 0f
        }
    }

    // Track the fraction as the reader scrolls. It is recomputed whenever the
    // layout changes too, so a page that fits entirely on screen (maxValue 0)
    // still counts as read. Completion is sticky, so a viewport resize during
    // teardown can no longer lower a finished chapter.
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(scroll.value, scroll.maxValue, scroll.isScrollInProgress) }
            .collect { (_, _, _) ->
                if (!screenActive.value) return@collect
                val fraction = scroll.fraction(endPaddingPx)
                latestFraction.value = fraction
                if (!reachedEnd.value &&
                    state.page >= state.pagesCount &&
                    fraction >= 0.999f
                ) {
                    // Reaching the end of the last page forces every later
                    // save to 100%, including the one made while the screen
                    // is being torn down.
                    reachedEnd.value = true
                    vm.markCompleted()
                }
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scroll.value }
            .debounce(1000)
            .collect {
                // Ignore a debounced write that arrives while the screen is
                // being torn down; the onDispose write below is the final one.
                if (screenActive.value) {
                    vm.saveScroll(if (reachedEnd.value) 1f else latestFraction.value)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            screenActive.value = false
            vm.saveScroll(if (reachedEnd.value) 1f else latestFraction.value)
        }
    }

    val paragraphs = remember(state.text) { splitParagraphs(state.text) }
    val progress by remember(state.page, state.pagesCount, endPaddingPx) {
        derivedStateOf {
            (((state.page - 1) + scroll.fraction(endPaddingPx)) / state.pagesCount.coerceAtLeast(1))
                .coerceIn(0f, 1f)
        }
    }
    val family = readerFontFamily(settings.fontFamily)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = state.title.ifEmpty { "Story" }, onBack = onBack) {
            val slug = state.summary?.slug
            if (!slug.isNullOrBlank()) {
                WebButton(
                    WebTarget(
                        url = container.routes.storyWebUrl(slug),
                        title = state.title.ifBlank { "Story" },
                    ),
                )
            }
            TextButton(onClick = { showSave = true }) { Text("Save") }
            TextButton(onClick = { showSettings = true }) {
                Text("Aa", style = MaterialTheme.typography.titleMedium)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val err = state.error
            when {
                state.loading -> LoadingBox()
                err != null -> ErrorBox(err, onRetry = { vm.retry() })
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        state.author?.let {
                            Text(
                                text = "by $it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 14.dp),
                            )
                        }
                        paragraphs.forEachIndexed { index, paragraph ->
                            Text(
                                text = paragraph,
                                fontSize = settings.fontSizeSp.sp,
                                lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                                fontFamily = family,
                                textAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start,
                                // Do not add trailing space after the final
                                // paragraph; the progress calculation treats
                                // the end of the text as the end of the page.
                                modifier = Modifier.padding(
                                    bottom = if (index == paragraphs.lastIndex) 0.dp else 16.dp,
                                ),
                            )
                        }
                    }
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(3.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalIconButton(
                onClick = { vm.previous() },
                enabled = !state.loading && state.page > 1,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
            }
            Text(
                text = "Page ${state.page} of ${state.pagesCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalIconButton(
                onClick = { vm.next() },
                enabled = !state.loading && state.page < state.pagesCount,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            ) {
                Text("Reading settings", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                ReadingSettingsPanel(container, settings)
            }
        }
    }
    if (showSave) {
        SaveToListDialog(
            container = container,
            storyId = storyId,
            savedLocally = state.savedLocally,
            onSaveLocally = { vm.saveLocally() },
            onRemoveLocally = { vm.removeLocally() },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun SaveToListDialog(
    container: AppContainer,
    storyId: Long,
    savedLocally: Boolean,
    onSaveLocally: () -> Unit,
    onRemoveLocally: () -> Unit,
    onDismiss: () -> Unit,
) {
    val auth by container.session.state.collectAsStateWithLifecycle()
    val signedIn = auth is AuthState.SignedIn
    var lists by remember { mutableStateOf<List<UserList>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(signedIn) {
        if (signedIn) {
            try {
                lists = container.lists.myLists()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                message = e.toApiError().userMessage()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Save story") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("On this device", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Kept locally. No account needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { if (savedLocally) onRemoveLocally() else onSaveLocally() },
                ) {
                    Text(if (savedLocally) "Remove from local list" else "Save to local list")
                }

                Spacer(Modifier.height(12.dp))
                Text("Account lists", style = MaterialTheme.typography.titleSmall)
                val current = lists
                when {
                    !signedIn -> Text(
                        text = "Sign in from the My Lists tab to save to your account lists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    current == null && message == null -> CircularProgressIndicator()
                    current != null && current.isEmpty() -> Text(
                        text = "You have no account lists yet. Create one from the My Lists tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    current != null -> current.forEach { list ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    message = try {
                                        container.lists.addStory(storyId, list.id)
                                        "Added to ${list.title}"
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Throwable) {
                                        e.toApiError().userMessage()
                                    }
                                }
                            },
                        ) { Text(list.title) }
                    }
                }
                message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
    )
}
