package dev.erolit.reader.core.net

/**
 * Global politeness gate: every HTTP call, including images, waits until at least
 * [minIntervalMs] has elapsed since the previous call in this process.
 *
 * The limiter is intentionally blocking because OkHttp interceptors are synchronous.
 * A single shared instance is used by both the API client and the image client.
 */
class RateLimiter(
    @Volatile var minIntervalMs: Long = 1_000L,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
    private val sleeper: (Long) -> Unit = { millis -> if (millis > 0) Thread.sleep(millis) },
) {
    private val lock = Any()
    private var lastRequestAtMs: Long = Long.MIN_VALUE / 2

    fun awaitSlot() {
        synchronized(lock) {
            val now = clock()
            val wait = (lastRequestAtMs + minIntervalMs) - now
            if (wait > 0) sleeper(wait)
            lastRequestAtMs = clock()
        }
    }

    fun resetForTest() {
        synchronized(lock) { lastRequestAtMs = Long.MIN_VALUE / 2 }
    }
}
