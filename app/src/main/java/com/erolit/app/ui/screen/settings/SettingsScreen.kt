package com.erolit.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.erolit.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var darkMode by rememberSaveable { mutableStateOf(true) }
    var wifiOnly by rememberSaveable { mutableStateOf(true) }
    var fontSize by rememberSaveable { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "Account") {
                    SettingsRow(title = "Login", onClick = { navController.navigate(Screen.Login.route) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(title = "Log Out", onClick = {})
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsRow(
                        title = "Dark Mode",
                        trailing = { 
                            Switch(
                                checked = darkMode, 
                                onCheckedChange = { darkMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            ) 
                        },
                        onClick = { darkMode = !darkMode }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "Primary Color",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ColorPill("AA", androidx.compose.ui.graphics.Color(0xFFD69151))
                                ColorPill("82", androidx.compose.ui.graphics.Color(0xFF5BAC81))
                                ColorPill("D6", androidx.compose.ui.graphics.Color(0xFF8A6BB4))
                            }
                        },
                        onClick = {}
                    )
                }
            }

            item {
                SettingsSection(title = "Reading") {
                    SettingsRow(
                        title = "Font Type",
                        trailing = { Text("Font Name", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {}
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "Font Size",
                        subtitle = {
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 0f..2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.2f)
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Storage") {
                    SettingsRow(
                        title = "Download over Wi-Fi only",
                        trailing = { 
                            Switch(
                                checked = wifiOnly, 
                                onCheckedChange = { wifiOnly = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            ) 
                        },
                        onClick = { wifiOnly = !wifiOnly }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "Clear Cache",
                        titleColor = MaterialTheme.colorScheme.primary,
                        onClick = {}
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsRow(
                        title = "Version",
                        trailing = { Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {}
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "Github Repository",
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            if (trailing != null) {
                trailing()
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            subtitle()
        }
    }
}

@Composable
fun ColorPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.9f))
    }
}
