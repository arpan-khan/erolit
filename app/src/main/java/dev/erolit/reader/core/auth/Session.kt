package dev.erolit.reader.core.auth

import dev.erolit.reader.core.net.AppJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64

@Serializable
data class SessionState(
    @SerialName("session_id") val sessionId: String,
    @SerialName("auth_token") val authToken: String? = null,
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("user_id") val userId: Long = 0L,
    val username: String = "",
)

interface SecretStore {
    suspend fun load(): SessionState?
    suspend fun save(state: SessionState)
    suspend fun clear()
}

sealed interface AuthState {
    data object Loading : AuthState
    data object Anonymous : AuthState
    data class SignedIn(val username: String, val userId: Long) : AuthState
    data object Expired : AuthState
}

sealed interface LoginOutcome {
    data class Success(val username: String) : LoginOutcome
    data class Failure(val message: String) : LoginOutcome
}

/**
 * Decodes only `exp` and `preferred_username` from the short-lived token. Never logs
 * the token or any other claim.
 */
object Jwt {
    fun expiryEpochSeconds(token: String): Long? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val bytes = Base64.getUrlDecoder().decode(padded)
            val obj = AppJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            obj["exp"]?.jsonPrimitive?.longOrNull
        }.getOrNull()
    }

    fun preferredUsername(token: String): String? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val bytes = Base64.getUrlDecoder().decode(padded)
            val obj = AppJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            obj["preferred_username"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}

private val kotlinx.serialization.json.JsonElement.contentOrNull: String?
    get() = runCatching { jsonPrimitive.content }.getOrNull()
