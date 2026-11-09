package dev.erolit.reader.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * A small read-only rating chip: ★★★★☆ 4.41 · 49 reviews.
 * Not clickable itself; wrap it where a content description is needed.
 */
@Composable
fun Rating(
    rating: Double?,
    count: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        val stars = rating?.let { Math.round(it * 2) / 2.0 } ?: 0.0
        var whole = stars.toInt()
        Text(
            text = "${"★".repeat(whole.coerceAtLeast(0).coerceAtMost(5))}${"☆".repeat(5 - whole.coerceAtMost(5))}${"☆".repeat(5 - whole.coerceAtLeast(0))}",
            style = MaterialTheme.typography.bodyMedium,
        )
        rating?.let {
            Text(
                text = " ${String.format("%.2f", it)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        count?.let {
            Text(
                text = " ($it)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}
