package dev.erolit.reader.core.auth

import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiResponse
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.TokenProvider
import dev.erolit.reader.data.dto.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SessionRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val secretStore: SecretStore,
    private val db: AppDatabase,
    private val appScope: CoroutineScope,
    private val now: () -> Long = { System.currentTimeMillis() },
) : TokenProvider {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    @Volatile
    private var session: SessionState? = null

    private val refreshMutex = Mutex()

    init {
        appScope.launch { restore() }
    }

    fun currentSession(): SessionState? = session

    /** Username for display only; never an email address. */
    fun signedInUsername(): String? = session?.username?.takeIf { it.isNotBlank() }

    fun currentUserId(): Long? = session?.userId?.takeIf { it > 0 }

    override fun cachedToken(): String? = session?.authToken

    override suspend fun ensureFresh(): Boolean {
        val current = session ?: return false
        if (current.authToken.isNullOrBlank() || current.expiresAt - now() < 60_000L) {
            return refreshNow()
        }
        return true
    }

    override suspend fun refreshNow(): Boolean = refreshMutex.withLock {
        val current = session ?: return@withLock false
        val response = try {
            api.performRaw(
                method = "GET",
                url = routes.checkUrl(now() / 1_000L),
                headers = mapOf(
                    "Cookie" to "sessionid=${current.sessionId}",
                    "Accept" to "*/*",
                ),
            )
        } catch (e: Exception) {
            // Transient network failure: keep the session, let the caller surface an error.
            return@withLock false
        }

        if (response.code == 401 || response.code == 403) {
            clearSession(AuthState.Expired)
            return@withLock false
        }
        if (response.code !in 200..299) return@withLock false

        val token = response.body.trim().takeIf { looksLikeJwt(it) }
            ?: cookieValue(response, "auth_token")
        if (token.isNullOrBlank()) {
            clearSession(AuthState.Expired)
            return@withLock false
        }

        val rotatedSessionId = cookieValue(response, "sessionid") ?: current.sessionId
        val expiresAt = Jwt.expiryEpochSeconds(token)?.times(1_000L)
            ?: (now() + 50L * 60L * 1_000L)
        val updated = current.copy(
            sessionId = rotatedSessionId,
            authToken = token,
            expiresAt = expiresAt,
        )
        session = updated
        runCatching { secretStore.save(updated) }
        if (updated.username.isNotBlank()) {
            _state.value = AuthState.SignedIn(updated.username, updated.userId)
        }
        true
    }

    override fun onAuthExpired() {
        session = null
        _state.value = AuthState.Expired
        appScope.launch { runCatching { secretStore.clear() } }
    }

    suspend fun login(username: String, password: String): LoginOutcome {
        if (username.isBlank() || password.isBlank()) {
            return LoginOutcome.Failure("Enter your username and password.")
        }
        val body = api.formBody(
            mapOf(
                "login" to username,
                "password" to password,
                "return_to" to "/",
                "form_url" to routes.loginFormUrl(),
            ),
        )
        val response = try {
            api.performRaw(
                method = "POST",
                url = routes.loginUrl(),
                body = body,
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Origin" to routes.loginOrigin(),
                    "Referer" to routes.loginFormUrl(),
                    "Accept" to "text/html,application/xhtml+xml",
                ),
            )
        } catch (e: ApiError) {
            return LoginOutcome.Failure("Could not reach the sign-in service. Check your connection.")
        }

        failureMessage(response.header("Location"))?.let { return LoginOutcome.Failure(it) }

        if (response.contentType?.contains("text/html", ignoreCase = true) == true && response.code == 200) {
            return LoginOutcome.Failure("The sign-in service asked for a browser check. Please try again later.")
        }
        if (response.code !in 200..399) {
            return LoginOutcome.Failure("Sign-in failed (server response ${response.code}).")
        }

        val sessionId = cookieValue(response, "sessionid")
            ?: return LoginOutcome.Failure("Sign-in could not be verified. Please try again.")

        val token = cookieValue(response, "auth_token")
        var provisional = SessionState(
            sessionId = sessionId,
            authToken = token,
            expiresAt = token?.let { Jwt.expiryEpochSeconds(it)?.times(1_000L) } ?: 0L,
            userId = 0L,
            username = "",
        )
        session = provisional

        if (token.isNullOrBlank() || provisional.expiresAt - now() < 60_000L) {
            val refreshed = refreshNow()
            if (!refreshed) {
                clearSession(AuthState.Expired)
                return LoginOutcome.Failure("Signed in, but the session could not be verified. Please try again.")
            }
        }
        provisional = session ?: return LoginOutcome.Failure("Sign-in could not be verified. Please try again.")

        val user = try {
            api.getJson(
                url = routes.session(),
                authenticated = true,
                retryTransient404 = false,
            ) { raw -> AppJson.decodeFromString<UserDto>(raw) }
        } catch (e: ApiError) {
            clearSession(AuthState.Expired)
            return LoginOutcome.Failure(
                if (e is ApiError.Network) "Network error while loading your profile. Please try again." else "Could not load your profile. Please try again.",
            )
        }

        val completed = provisional.copy(
            userId = user.userid,
            username = user.username,
        )
        session = completed
        runCatching { secretStore.save(completed) }
        _state.value = AuthState.SignedIn(completed.username, completed.userId)
        return LoginOutcome.Success(completed.username)
    }

    suspend fun logout() {
        val old = session
        clearSession(AuthState.Anonymous)
        runCatching { db.storyDao().clearAllPages() }
        if (old != null) {
            appScope.launch {
                runCatching {
                    api.performRaw(
                        method = "POST",
                        url = routes.logoutUrl(),
                        headers = mapOf(
                            "Cookie" to "sessionid=${old.sessionId}",
                            "Accept" to "*/*",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun restore() {
        val stored = try {
            secretStore.load()
        } catch (e: Exception) {
            null
        }
        if (stored == null) {
            _state.value = AuthState.Anonymous
            return
        }
        session = stored
        _state.value = if (stored.username.isNotBlank()) {
            AuthState.SignedIn(stored.username, stored.userId)
        } else {
            AuthState.Anonymous
        }
        if (stored.expiresAt - now() < 60_000L) {
            refreshNow()
        }
    }

    private suspend fun clearSession(newState: AuthState) {
        session = null
        runCatching { secretStore.clear() }
        _state.value = newState
    }

    private fun cookieValue(response: ApiResponse, name: String): String? =
        response.headerValues("Set-Cookie")
            .firstOrNull { it.substringBefore('=').trim().equals(name, ignoreCase = true) }
            ?.substringAfter('=', "")
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun failureMessage(location: String?): String? {
        if (location.isNullOrBlank()) return null
        val raw = Regex("[?&]message=([^&]+)").find(location)?.groupValues?.getOrNull(1) ?: return null
        return runCatching {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
        }.getOrDefault(raw).takeIf { it.isNotBlank() }
    }

    private fun looksLikeJwt(value: String): Boolean =
        value.length > 40 && value.count { it == '.' } == 2 && value.startsWith("ey")
}
