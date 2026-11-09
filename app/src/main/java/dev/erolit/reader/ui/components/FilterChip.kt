package dev.erolit.reader.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

/**
 * A small selectable filter chip built on material3 Button.
 */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
    ) {
        label()
    }
}
