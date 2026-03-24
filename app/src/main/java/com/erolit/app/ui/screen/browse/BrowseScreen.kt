package com.erolit.app.ui.screen.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.erolit.app.domain.model.AllCategories
import com.erolit.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    "Popular Tags",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            item(span = { GridItemSpan(2) }) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val popularTags = listOf("Mom", "Son", "Milf", "Slow Burn", "Oral", "Masturbation", "First Time", "Cheating", "Creampie", "Voyeur")
                    popularTags.forEach { tag ->
                        TagPill(tag = tag, onClick = { navController.navigate(Screen.TagDetail.createRoute(tag.lowercase())) })
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "All Categories",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            items(AllCategories, key = { it.slug }) { category ->
                CategoryGradientCard(
                    categoryName = category.name,
                    onClick = { navController.navigate(Screen.CategoryDetail.createRoute(category.slug)) }
                )
            }
        }
    }
}

@Composable
fun TagPill(tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryGradientCard(categoryName: String, onClick: () -> Unit) {
    val gradientColors = getGradientForCategory(categoryName)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // Tall rectangular cards like mockup
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors)
                )
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getGradientForCategory(name: String): List<Color> {
    return when (name.lowercase()) {
        "historical" -> listOf(Color(0xFFB69E81), Color(0xFF72A2B2))
        "contemporary" -> listOf(Color(0xFFAAB7CD), Color(0xFF6385A2))
        "thriller" -> listOf(Color(0xFFD47A7F), Color(0xFF70628D))
        "paranormal" -> listOf(Color(0xFFA993B0), Color(0xFF686989))
        "young adult" -> listOf(Color(0xFFDED381), Color(0xFF7E9B8D))
        "romantic" -> listOf(Color(0xFFD7A779), Color(0xFF8B7A6A)) // Mapping an extra one
        else -> listOf(Color(0xFF6C8BB8), Color(0xFF3B5998))
    }
}
