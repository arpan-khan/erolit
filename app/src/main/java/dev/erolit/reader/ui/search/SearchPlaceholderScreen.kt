package dev.erolit.reader.ui.search

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
fun SearchPlaceholderScreen(container: AppContainer) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Text(
            text = "Search — Phase 4\nStories · Poetry · Members",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp).semantics { contentDescription = "Search placeholder" },
        )
    }
}
