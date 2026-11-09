package dev.erolit.reader.core.net

interface TokenProvider {
    fun cachedToken(): String?

    /** Cheap when the token is fresh; may refresh when it is missing or close to expiry. */
    suspend fun ensureFresh(): Boolean

    /** Forces one refresh attempt. */
    suspend fun refreshNow(): Boolean

    /** Called when a refresh failed; the user must sign in again. */
    fun onAuthExpired()
}

internal object AuthRequired

object UserAgent {
    /** Honest, app-specific identity. Never rotated and never used to evade a block. */
    const val VALUE = "Erolit/0.1 (Android; personal reader)"
}
