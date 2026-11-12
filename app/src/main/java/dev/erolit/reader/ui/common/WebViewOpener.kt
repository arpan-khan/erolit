package dev.erolit.reader.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens a page in the in-app WebView. Every screen under the nav root can call
 * this without threading a callback through the screen hierarchy.
 */
data class WebTarget(
    val url: String,
    val title: String? = null,
)

val LocalOpenWeb = staticCompositionLocalOf<(WebTarget) -> Unit> { {} }
