package com.erolit.app.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Wrap each emission in Result.success, mapping exceptions to Result.failure. */
fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    map { Result.success(it) }
        .catch { emit(Result.failure(it)) }

/** Compact number formatting for display (e.g. 1500 -> "1.5k"). */
fun Int.toCompactString(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000}M"
    this >= 1_000 -> "${this / 1_000}k"
    else -> toString()
}

/** Format a rating float to display string "4.67". */
fun Float.toRatingString(): String = String.format("%.2f", this)
