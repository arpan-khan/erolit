package dev.erolit.reader.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.auth.AuthState
import dev.erolit.reader.core.backup.BackupStats
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.components.ScreenHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val auth by container.session.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var backupFailed by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                backupFailed = false
                backupStatus = "Writing backup…"
                try {
                    val result = withContext(Dispatchers.IO) {
                        val data = container.backup.export()
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Could not open the selected file.")
                        output.use { it.write(data.json.toByteArray(Charsets.UTF_8)) }
                        data
                    }
                    backupStatus = "Backup saved: ${backupSummary(result.stats)}."
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    backupFailed = true
                    backupStatus = e.message ?: "Could not export the backup."
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                backupFailed = false
                backupStatus = "Restoring backup…"
                try {
                    val restored = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Could not open the selected file.")
                        val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        container.backup.import(raw)
                    }
                    backupStatus = "Backup restored: ${backupSummary(restored)}."
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    backupFailed = true
                    backupStatus = e.message ?: "Could not restore the backup."
                }
            }
        }
    }

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
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Export your local lists, reading progress, history, and reading settings " +
                    "to a file, or restore them on this or another device. Account credentials " +
                    "are never included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    backupFailed = false
                    exportLauncher.launch(defaultBackupFileName())
                }) { Text("Export backup") }
                TextButton(onClick = { confirmImport = true }) { Text("Import backup") }
            }
            backupStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backupFailed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

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

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "This replaces saved stories, series, authors, reading progress, history, " +
                        "and reading settings on this device. Your account sign-in is not changed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream"),
                    )
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancel") } },
        )
    }
}

private fun backupSummary(stats: BackupStats): String = listOf(
    "${stats.stories} saved stor${if (stats.stories == 1) "y" else "ies"}",
    "${stats.series} series",
    "${stats.authors} author${if (stats.authors == 1) "" else "s"}",
    "${stats.historyEntries} history ${if (stats.historyEntries == 1) "entry" else "entries"}",
).joinToString(", ")

private fun defaultBackupFileName(): String =
    "erolit-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json"
