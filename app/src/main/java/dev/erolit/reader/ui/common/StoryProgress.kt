package dev.erolit.reader.ui.common

import androidx.compose.runtime.compositionLocalOf
import dev.erolit.reader.domain.ProgressInfo

/**
 * Reading progress for every story known on this device, keyed by story id.
 * Provided once at the navigation root so every story list can show the same
 * live percentages without each screen collecting its own database flow.
 */
val LocalStoryProgress = compositionLocalOf<Map<Long, ProgressInfo>> { emptyMap() }
