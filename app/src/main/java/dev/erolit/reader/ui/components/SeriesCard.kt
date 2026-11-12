package dev.erolit.reader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.erolit.reader.domain.ProgressInfo
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.ui.LocalAppContainer
import dev.erolit.reader.ui.common.LocalStoryProgress
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A compact series entry with a letter tile instead of cover art. Series on
 * the upstream service do not have images, so the tile keeps the list readable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCard(
    series: SeriesSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthor: (() -> Unit)? = null,
    saved: Boolean = false,
    extraActions: List<StoryAction> = emptyList(),
) {
    var showActions by remember { mutableStateOf(false) }
    val progress = LocalStoryProgress.current
    val percent = seriesProgressPercent(series.workIds, progress)?.takeIf { it > 0 }
    val completed = percent != null && percent >= 100

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClickLabel = "Open series ${series.title}",
                onLongClickLabel = "Show actions for ${series.title}",
                role = Role.Button,
                onClick = onClick,
                onLongClick = { showActions = true },
            ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = series.title.trim().firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (completed) 0.6f else 1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (percent != null) {
                        Spacer(Modifier.width(8.dp))
                        SeriesProgressBadge(percent = percent, completed = completed)
                    }
                }
                val meta = listOfNotNull(
                    series.authorName,
                    series.workCount?.let { "$it chapter${if (it == 1) "" else "s"}" },
                    series.state?.replace('_', ' '),
                    if (saved) "Saved locally" else null,
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val description = series.description
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (onAuthor != null || extraActions.isNotEmpty()) {
                IconButton(
                    onClick = { showActions = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions for ${series.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                )
            }
        }
    }

    if (showActions) {
        SeriesActionSheet(
            series = series,
            onAuthor = onAuthor,
            extraActions = extraActions,
            onDismiss = { showActions = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesActionSheet(
    series: SeriesSummary,
    onAuthor: (() -> Unit)?,
    extraActions: List<StoryAction>,
    onDismiss: () -> Unit,
) {
    val container = LocalAppContainer.current
    val progress = LocalStoryProgress.current
    val scope = rememberCoroutineScope()
    val allRead = series.workIds.isNotEmpty() && series.workIds.all { id ->
        val value = progress[id]?.percent?.coerceIn(0f, 1f) ?: 0f
        (value * 100f).roundToInt() >= 100
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        ) {
            Text(series.title, style = MaterialTheme.typography.titleLarge)
            val meta = listOfNotNull(
                series.authorName,
                series.workCount?.let { "$it chapters" },
                series.state?.replace('_', ' '),
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            if (onAuthor != null) {
                SeriesActionRow(
                    icon = Icons.Default.Person,
                    label = "View author",
                    supporting = series.authorName ?: "Author profile",
                ) {
                    onDismiss()
                    onAuthor()
                }
            }
            SeriesActionRow(
                icon = if (allRead) Icons.Default.Close else Icons.Default.Check,
                label = if (allRead) "Mark as unread" else "Mark as read",
                supporting = if (allRead) {
                    "Clear progress for every chapter"
                } else {
                    "Mark every chapter as finished"
                },
            ) {
                onDismiss()
                scope.launch {
                    if (allRead) {
                        container.stories.markSeriesUnread(series.id, series.workIds)
                    } else {
                        container.stories.markSeriesRead(series.id, series.workIds)
                    }
                }
            }
            extraActions.forEach { action ->
                SeriesActionRow(icon = action.icon, label = action.label, supporting = action.supporting) {
                    onDismiss()
                    action.onClick()
                }
            }
        }
    }
}

@Composable
private fun SeriesActionRow(
    label: String,
    supporting: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp)),
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
            Text(label, style = MaterialTheme.typography.bodyLarge)
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

/**
 * Series progress is the average of its chapters' reading percentages, so it
 * reaches 100% only once every chapter has been finished.
 */
private fun seriesProgressPercent(
    workIds: List<Long>,
    progress: Map<Long, ProgressInfo>,
): Int? {
    if (workIds.isEmpty()) return null
    val total = workIds.sumOf { id ->
        (progress[id]?.percent?.coerceIn(0f, 1f) ?: 0f).toDouble()
    }
    return ((total / workIds.size) * 100.0).roundToInt().coerceIn(0, 100)
}

@Composable
fun SeriesProgressBadge(
    percent: Int,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = if (completed) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (completed) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = if (completed) "Read" else "$percent%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** Spacer used by the empty-actions branch to keep the row height stable. */
@Composable
private fun SeriesSpacer() {
    Spacer(Modifier.height(0.dp))
}
