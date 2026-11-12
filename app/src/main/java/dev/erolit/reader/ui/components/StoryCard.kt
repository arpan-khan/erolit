package dev.erolit.reader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.erolit.reader.data.formatCompact
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.ui.LocalAppContainer
import dev.erolit.reader.ui.common.LocalSavedStoryIds
import dev.erolit.reader.ui.common.LocalStoryNavigation
import dev.erolit.reader.ui.common.LocalStoryProgress
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/** An extra row shown in a story's long-press action sheet. */
data class StoryAction(
    val label: String,
    val supporting: String? = null,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryCard(
    story: StorySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingLabel: String? = null,
    leadingMeta: String? = null,
    extraActions: List<StoryAction> = emptyList(),
) {
    var showActions by remember { mutableStateOf(false) }
    val progress = LocalStoryProgress.current[story.id]
    val percent = progress?.let { (it.percent.coerceIn(0f, 1f) * 100f).roundToInt() }
    val completed = percent != null && percent >= 100
    val textAlpha = if (completed) 0.6f else 1f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClickLabel = "Open ${story.title}",
                onLongClickLabel = "Show actions for ${story.title}",
                role = Role.Button,
                onClick = onClick,
                onLongClick = { showActions = true },
            ),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (leadingLabel != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = leadingLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (percent != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (completed) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (completed) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    ) {
                        Text(
                            text = if (completed) "Read" else "$percent%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { showActions = true },
                    modifier = Modifier.size(36.dp).padding(start = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions for ${story.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val byline = listOfNotNull(leadingMeta, story.authorName, story.categoryName, story.publishedAt)
                .joinToString(" · ")
            if (byline.isNotEmpty()) {
                Text(
                    text = byline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.dp, top = 1.dp),
                )
            }

            val description = story.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.dp, top = 6.dp),
                )
            }

            StoryMetaRow(story, textAlpha = textAlpha, modifier = Modifier.padding(end = 10.dp, top = 8.dp))
        }
    }

    if (showActions) {
        StoryActionSheet(
            story = story,
            extraActions = extraActions,
            onDismiss = { showActions = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryMetaRow(story: StorySummary, textAlpha: Float, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        story.rating?.let {
            MetaPill(
                text = "★ " + String.format(Locale.US, "%.2f", it),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                alpha = textAlpha,
            )
        }
        story.words?.let {
            MetaPill(
                text = formatCompact(it) + " words",
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                alpha = textAlpha,
            )
        }
        if (story.isHot) {
            MetaPill(
                text = "Hot",
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                alpha = textAlpha,
            )
        }
        if (story.isNew) {
            MetaPill(
                text = "New",
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                alpha = textAlpha,
            )
        }
        if (story.writersPick) {
            MetaPill(
                text = "Writer's pick",
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                alpha = textAlpha,
            )
        }
        story.seriesTitle?.let {
            MetaPill(
                text = it,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
                alpha = textAlpha,
            )
        }
    }
}

@Composable
private fun MetaPill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    Surface(shape = CircleShape, color = color.copy(alpha = color.alpha * alpha)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = contentColor.alpha * alpha),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryActionSheet(
    story: StorySummary,
    onDismiss: () -> Unit,
    extraActions: List<StoryAction> = emptyList(),
) {
    val nav = LocalStoryNavigation.current
    val container = LocalAppContainer.current
    val saved = story.id in LocalSavedStoryIds.current
    val storyProgress = LocalStoryProgress.current[story.id]
    val isRead = storyProgress != null &&
        (storyProgress.percent.coerceIn(0f, 1f) * 100f).roundToInt() >= 100
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeThen(action: () -> Unit) {
        onDismiss()
        action()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val byline = listOfNotNull(
                story.authorName,
                story.categoryName,
                story.words?.let { formatCompact(it) + " words" },
                story.rating?.let { "★ " + String.format(Locale.US, "%.2f", it) },
            ).joinToString(" · ")
            if (byline.isNotEmpty()) {
                Text(
                    text = byline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (story.tags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    story.tags.take(8).forEach { tag ->
                        AssistChip(
                            onClick = { nav?.let { closeThen { it.openTag(tag) } } },
                            label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            val authorId = story.authorId
            if (authorId != null && nav != null) {
                ActionRow(
                    icon = Icons.Default.Person,
                    label = "View author",
                    supporting = story.authorName ?: "Author profile",
                ) { closeThen { nav.openAuthor(authorId) } }
            }
            val seriesId = story.seriesId
            if (seriesId != null && nav != null) {
                ActionRow(
                    icon = Icons.Default.List,
                    label = "Open series",
                    supporting = story.seriesTitle ?: "Series",
                ) { closeThen { nav.openSeries(seriesId) } }
            }
            val categoryId = story.categoryId
            if (categoryId != null && nav != null) {
                ActionRow(
                    icon = Icons.Default.List,
                    label = "Browse category",
                    supporting = story.categoryName ?: "Category",
                ) { closeThen { nav.openCategory(categoryId, story.categoryName ?: "Category") } }
            }
            if (story.tags.isNotEmpty() && nav != null) {
                ActionRow(
                    icon = Icons.Default.Search,
                    label = "Search all tags",
                    supporting = "Find more stories like this",
                ) {
                    closeThen {
                        val query = story.tags.firstOrNull().orEmpty()
                        if (query.isNotBlank()) nav.openTag(query)
                    }
                }
            }

            if (nav != null || extraActions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            }

            ActionRow(
                icon = if (saved) Icons.Default.Check else Icons.Default.List,
                label = if (saved) "Remove from local list" else "Save for local reading",
                supporting = if (saved) {
                    "This story is stored on this device"
                } else {
                    "Stored on this device. No account needed."
                },
            ) {
                onDismiss()
                scope.launch {
                    if (saved) container.stories.removeLocal(story.id)
                    else container.stories.saveLocal(story)
                }
            }

            ActionRow(
                icon = if (isRead) Icons.Default.Close else Icons.Default.Check,
                label = if (isRead) "Mark as unread" else "Mark as read",
                supporting = if (isRead) {
                    "Clear this story's reading progress"
                } else {
                    "Mark this story as finished"
                },
            ) {
                onDismiss()
                scope.launch {
                    if (isRead) container.stories.markStoryUnread(story.id)
                    else container.stories.markStoryRead(story.id)
                }
            }

            extraActions.forEach { action ->
                ActionRow(
                    icon = action.icon,
                    label = action.label,
                    supporting = action.supporting,
                ) { closeThen(action.onClick) }
            }
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    supporting: String?,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
