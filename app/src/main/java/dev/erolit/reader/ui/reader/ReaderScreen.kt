package dev.erolit.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
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
import dev.erolit.reader.ui.components.ErrorBox
import dev.erolit.reader.ui.components.LoadingBox
import dev.erolit.reader.ui.components.ScreenHeader
import dev.erolit.reader.ui.settings.ReadingSettingsPanel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun ScrollState.fraction(): Float = when {
    maxValue in 1 until Int.MAX_VALUE -> (value / maxValue.toFloat()).coerceIn(0f, 1f)
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

@OptIn(FlowPreview::class)
@Composable
fun ReaderScreen(container: AppContainer, storyId: Long, onBack: () -> Unit) {
    val vm: ReaderViewModel = viewModel(
        key = "reader-$storyId",
        factory = vmFactory { ReaderViewModel(container.stories, storyId, container.appScope) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scroll = rememberScrollState()
    var showSettings by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val view = LocalView.current

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
        } else {
            scroll.scrollTo(0)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scroll.value }
            .debounce(1000)
            .collect { vm.saveScroll(scroll.fraction()) }
    }

    DisposableEffect(Unit) {
        onDispose { vm.saveScroll(scroll.fraction()) }
    }

    val paragraphs = remember(state.text) { splitParagraphs(state.text) }
    val progress by remember(state.page, state.pagesCount) {
        derivedStateOf {
            (((state.page - 1) + scroll.fraction()) / state.pagesCount.coerceAtLeast(1))
                .coerceIn(0f, 1f)
        }
    }
    val family = when (settings.fontFamily) {
        "sans" -> FontFamily.SansSerif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.Serif
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = state.title.ifEmpty { "Story" }, onBack = onBack) {
            TextButton(onClick = { showSave = true }) { Text("Save") }
            TextButton(onClick = { showSettings = true }) { Text("Aa") }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val err = state.error
            when {
                state.loading -> LoadingBox()
                err != null -> ErrorBox(err, onRetry = { vm.retry() })
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    state.author?.let {
                        Text(
                            text = "by $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                            fontFamily = family,
                            textAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { vm.previous() }, enabled = !state.loading && state.page > 1) {
                Text("Previous")
            }
            Text("Page ${state.page} of ${state.pagesCount}", style = MaterialTheme.typography.labelLarge)
            Button(
                onClick = { vm.next() },
                enabled = !state.loading && state.page < state.pagesCount,
            ) { Text("Next") }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } },
            title = { Text("Reading settings") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ReadingSettingsPanel(container, settings)
                }
            },
        )
    }
    if (showSave) {
        SaveToListDialog(
            container = container,
            storyId = storyId,
            savedOffline = state.savedOffline,
            onSaveOffline = { vm.saveOffline() },
            onRemoveOffline = { vm.removeOffline() },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun SaveToListDialog(
    container: AppContainer,
    storyId: Long,
    savedOffline: Boolean,
    onSaveOffline: () -> Unit,
    onRemoveOffline: () -> Unit,
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
                    onClick = { if (savedOffline) onRemoveOffline() else onSaveOffline() },
                ) {
                    Text(if (savedOffline) "Remove from offline list" else "Save to offline list")
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
