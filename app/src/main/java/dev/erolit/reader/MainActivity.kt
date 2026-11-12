package dev.erolit.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.erolit.reader.core.net.userMessage
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.core.update.UpdateInfo
import dev.erolit.reader.ui.LocalAppContainer
import dev.erolit.reader.ui.age.AgeGateScreen
import dev.erolit.reader.ui.navigation.AppNavHost
import dev.erolit.reader.ui.theme.ErolitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the system splash screen until the first settings snapshot is
        // ready, so the app is already composed underneath when it is removed.
        splashScreen.setKeepOnScreenCondition { !settingsLoaded }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.iconView?.animate()
                ?.scaleX(1.16f)
                ?.scaleY(1.16f)
                ?.setDuration(420L)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()
            provider.view.animate()
                .alpha(0f)
                .setStartDelay(150L)
                .setDuration(280L)
                .withEndAction { provider.remove() }
                .start()
        }

        enableEdgeToEdge()
        val container = (application as ErolitApp).container

        setContent {
            val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settingsState
            LaunchedEffect(current != null) {
                if (current != null) settingsLoaded = true
            }
            if (current == null) {
                Box(modifier = Modifier.fillMaxSize())
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
                        UpdateCheckHost(container, current.ignoredUpdateVersion)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateCheckHost(container: AppContainer, ignoredVersion: String?) {
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        update = container.updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
    }

    val info = update
    if (info != null && info.version != ignoredVersion) {
        AlertDialog(
            onDismissRequest = { update = null },
            title = { Text("Update available") },
            text = {
                Text(
                    "Erolit ${info.version.removePrefix("v")} is available. " +
                        "You are using version ${BuildConfig.VERSION_NAME}.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        update = null
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                        }
                    },
                ) { Text("Open GitHub") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        update = null
                        scope.launch { container.settings.setIgnoredUpdateVersion(info.version) }
                    },
                ) { Text("Ignore") }
            },
        )
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
