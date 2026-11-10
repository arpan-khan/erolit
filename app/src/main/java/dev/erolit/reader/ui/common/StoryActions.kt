package dev.erolit.reader.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation targets reachable from a story card's long-press action sheet.
 * Provided once at the navigation root so individual cards stay simple.
 */
data class StoryNavigation(
    val openAuthor: (Long) -> Unit,
    val openSeries: (Long) -> Unit,
    val openCategory: (Int, String) -> Unit,
    val openTag: (String) -> Unit,
)

val LocalStoryNavigation = staticCompositionLocalOf<StoryNavigation?> { null }

/** Ids of stories in the on-device saved list, kept in sync at the nav root. */
val LocalSavedStoryIds = compositionLocalOf<Set<Long>> { emptySet() }
