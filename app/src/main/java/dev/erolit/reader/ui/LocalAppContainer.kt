package dev.erolit.reader.ui

import androidx.compose.runtime.staticCompositionLocalOf
import dev.erolit.reader.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not available in this composition")
}
