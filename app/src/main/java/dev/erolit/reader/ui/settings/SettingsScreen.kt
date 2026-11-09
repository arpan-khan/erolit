package dev.erolit.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.auth.AuthState
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.components.ScreenHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val auth by container.session.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmWipe by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings", onBack = null)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Reading", style = MaterialTheme.typography.titleMedium)
            ReadingSettingsPanel(container, settings)

            Spacer(Modifier.height(16.dp))
            Text("Home", style = MaterialTheme.typography.titleMedium)
            Text("Default tab", style = MaterialTheme.typography.labelLarge)
            ChipRow {
                Chip("New", settings.defaultHomeTab == "new") { scope.launch { container.settings.setDefaultHomeTab("new") } }
                Chip("Top", settings.defaultHomeTab == "top") { scope.launch { container.settings.setDefaultHomeTab("top") } }
                Chip("Series", settings.defaultHomeTab == "series") { scope.launch { container.settings.setDefaultHomeTab("series") } }
            }
            Text("Default top list", style = MaterialTheme.typography.labelLarge)
            ChipRow {
                Chip("Top rated", settings.defaultTopSort == "rating") { scope.launch { container.settings.setDefaultTopSort("rating") } }
                Chip("Most read", settings.defaultTopSort == "views") { scope.launch { container.settings.setDefaultTopSort("views") } }
                Chip("Favorites", settings.defaultTopSort == "favorite") { scope.launch { container.settings.setDefaultTopSort("favorite") } }
            }
            ChipRow {
                Chip("7 days", settings.defaultTopPeriod == "week") { scope.launch { container.settings.setDefaultTopPeriod("week") } }
                Chip("30 days", settings.defaultTopPeriod == "month") { scope.launch { container.settings.setDefaultTopPeriod("month") } }
                Chip("12 months", settings.defaultTopPeriod == "year") { scope.launch { container.settings.setDefaultTopPeriod("year") } }
                Chip("All time", settings.defaultTopPeriod == "all") { scope.launch { container.settings.setDefaultTopPeriod("all") } }
            }

            Spacer(Modifier.height(16.dp))
            Text("Network", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Slow mode (2 seconds between requests)", settings.slowMode) {
                scope.launch { container.settings.setSlowMode(it) }
            }

            Spacer(Modifier.height(16.dp))
            Text("Account and data", style = MaterialTheme.typography.titleMedium)
            val signedIn = auth as? AuthState.SignedIn
            if (signedIn != null) {
                Text("Signed in as ${signedIn.username}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { scope.launch { container.session.logout() } }) { Text("Sign out") }
            } else {
                Text("Not signed in.", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { confirmWipe = true }) { Text("Log out and clear data") }
            status?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildAnnotatedString {
                    append("Erolit is an unofficial, unaffiliated client based on ")
                    withLink(LinkAnnotation.Url("https://www.literotica.com")) {
                        append("Literotica (literotica.com)")
                    }
                    append(
                        ", an adult fiction site. It contains no stories. " +
                            "You are responsible for following the terms of the service you connect to.",
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Log out and clear data?") },
            text = { Text("This signs you out and deletes reading progress, search history and cached pages on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    scope.launch {
                        container.session.logout()
                        container.clearLocalData()
                        status = "Signed out and local data cleared."
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } },
        )
    }
}
