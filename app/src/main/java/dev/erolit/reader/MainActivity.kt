package dev.erolit.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.ui.LocalAppContainer
import dev.erolit.reader.ui.age.AgeGateScreen
import dev.erolit.reader.ui.navigation.AppNavHost
import dev.erolit.reader.ui.theme.ErolitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ErolitApp).container

        setContent {
            val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settingsState
            if (current == null) {
                Splash()
                return@setContent
            }
            ErolitTheme(theme = current.theme, dynamicColor = current.dynamicColor) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    if (!current.ageConfirmed) {
                        val scope = rememberCoroutineScope()
                        AgeGateScreen(
                            onConfirm = { scope.launch { container.settings.setAgeConfirmed(true) } },
                            onExit = { finish() },
                        )
                    } else {
                        AppNavHost(container)
                    }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// Replaced by the real Home in Phase 2; retained only as a network smoke test.
@Composable
fun SkeletonStatus(settings: AppSettings) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Not checked") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Erolit", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Network core ready", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            scope.launch {
                status = "Checking…"
                status = try {
                    val count = container.catalog.loadCategories(force = true).size
                    "Service reachable · $count categories"
                } catch (e: Exception) {
                    (e as? dev.erolit.reader.core.net.ApiError)?.userMessage() ?: "Unexpected error"
                }
            }
        }) {
            Text("Check connection")
        }
        Spacer(Modifier.height(12.dp))
        Text(status, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Default home tab: ${settings.defaultHomeTab}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
