package com.erolit.app.ui.screen.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.erolit.app.ui.components.FullScreenLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(seriesId: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Series") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Series content loaded here
        FullScreenLoader(modifier = Modifier.padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(tagName: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tagName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Tag stories loaded here
        FullScreenLoader(modifier = Modifier.padding(padding))
    }
}
