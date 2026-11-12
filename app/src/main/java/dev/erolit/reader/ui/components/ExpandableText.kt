package dev.erolit.reader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Body text clamped to [collapsedMaxLines] until the user taps "Read more".
 * The toggle only appears when the text actually overflows the clamped layout.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 4,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = text,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) overflows = result.hasVisualOverflow
            },
        )
        if (overflows || expanded) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
            ) {
                Text(if (expanded) "Read less" else "Read more")
            }
        }
    }
}
