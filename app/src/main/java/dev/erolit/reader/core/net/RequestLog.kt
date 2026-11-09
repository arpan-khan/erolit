package dev.erolit.reader.core.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Last request metadata for the diagnostics screen. Never includes headers or query values. */
class RequestLog {
    data class Entry(
        val path: String,
        val status: Int,
        val atEpochMs: Long,
    )

    private val _last = MutableStateFlow<Entry?>(null)
    val last: StateFlow<Entry?> = _last

    fun record(path: String, status: Int) {
        _last.value = Entry(path = path, status = status, atEpochMs = System.currentTimeMillis())
    }
}
