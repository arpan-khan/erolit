package dev.erolit.reader.ui.series

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.erolit.reader.AppContainer

@Composable
fun SeriesPlaceholderScreen(container: AppContainer, seriesId: Long) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Text(
            text = "Series detail — Phase 5\nSeries id: $seriesId",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp).semantics { contentDescription = "Series placeholder for series $seriesId" },
        )
    }
}
