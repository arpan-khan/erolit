package dev.erolit.reader.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApiResponse(
    val code: Int,
    val headers: Headers,
    val body: String,
    val contentType: String?,
) {
    fun header(name: String): String? = headers[name]
    fun headerValues(name: String): List<String> = headers.values(name)
}

/**
 * One polite OkHttp client for the whole app. All calls are serialised (one in flight),
 * share a global 1 req/s limiter, send a stable honest UA, and never log headers or
 * bodies. Authentication is Bearer-only; the long-lived session cookie is used only
 * by the auth repository for `/check` and logout.
 */
class ApiClient(
    private val rateLimiter: RateLimiter,
    private val requestLog: RequestLog,
    private val json: Json = AppJson,
    private val delayMillis: suspend (Long) -> Unit = { delay(it) },
) {
    var tokenProvider: TokenProvider? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(false)
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 1
                maxRequestsPerHost = 1
            },
        )
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(RateLimitInterceptor(rateLimiter))
        .addInterceptor(AuthInterceptor { tokenProvider })
        .addInterceptor(RequestLogInterceptor(requestLog))
        .build()

    suspend fun <T> getJson(
        url: String,
        authenticated: Boolean = false,
        retryTransient404: Boolean = true,
        decode: (String) -> T,
    ): T = decodeBody(
        perform("GET", url.toHttpUrl(), authenticated = authenticated, retryTransient404 = retryTransient404),
        decode,
    )

    suspend fun <T> postJson(
        url: String,
        jsonBody: String,
        authenticated: Boolean = true,
        retryTransient404: Boolean = false,
        decode: (String) -> T,
    ): T = decodeBody(
        perform(
            "POST",
            url.toHttpUrl(),
            body = jsonBody.toRequestBody("application/json".toMediaType()),
            authenticated = authenticated,
            retryTransient404 = retryTransient404,
        ),
        decode,
    )

    suspend fun <T> put(
        url: String,
        jsonBody: String? = null,
        authenticated: Boolean = true,
        retryTransient404: Boolean = false,
        decode: (String) -> T,
    ): T = decodeBody(
        perform(
            "PUT",
            url.toHttpUrl(),
            body = (jsonBody ?: "{}").toRequestBody("application/json".toMediaType()),
            authenticated = authenticated,
            retryTransient404 = retryTransient404,
        ),
        decode,
    )

    suspend fun <T> delete(
        url: String,
        authenticated: Boolean = true,
        retryTransient404: Boolean = false,
        decode: (String) -> T,
    ): T = decodeBody(
        perform("DELETE", url.toHttpUrl(), authenticated = authenticated, retryTransient404 = retryTransient404),
        decode,
    )

    /** Raw call for login/check/logout, where the caller must inspect redirects and cookies. */
    suspend fun performRaw(
        method: String,
        url: String,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
        authenticated: Boolean = false,
    ): ApiResponse = perform(
        method = method,
        url = url.toHttpUrl(),
        body = body,
        authenticated = authenticated,
        extraHeaders = headers,
        retryTransient404 = false,
    )

    fun formBody(fields: Map<String, String>): RequestBody =
        FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()

    private suspend fun perform(
        method: String,
        url: okhttp3.HttpUrl,
        body: RequestBody? = null,
        authenticated: Boolean = false,
        extraHeaders: Map<String, String> = emptyMap(),
        retryTransient404: Boolean = method == "GET",
    ): ApiResponse {
        var authRetried = false
        var notFoundRetries = 0

        while (true) {
            if (authenticated && !authRetried) {
                tokenProvider?.ensureFresh()
            }

            val request = Request.Builder()
                .url(url)
                .method(method, body)
                .apply {
                    if (authenticated) tag(AuthRequired::class.java, AuthRequired)
                    if (!extraHeaders.keys.any { it.equals("Accept", ignoreCase = true) }) {
                        header("Accept", "application/json")
                    }
                    extraHeaders.forEach { (key, value) -> header(key, value) }
                }
                .build()

            val response = try {
                withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute().use { raw ->
                        ApiResponse(
                            code = raw.code,
                            headers = raw.headers,
                            body = raw.body?.string().orEmpty(),
                            contentType = raw.header("Content-Type"),
                        )
                    }
                }
            } catch (e: IOException) {
                throw ApiError.Network(e)
            }

            val unauthorized = response.code == 401 ||
                (response.code == 403 && isPlainUnauthorized(response.body))
            if (authenticated && unauthorized && !authRetried) {
                authRetried = true
                val refreshed = tokenProvider?.refreshNow() == true
                if (refreshed) continue
                tokenProvider?.onAuthExpired()
                throw ApiError.SessionExpired
            }

            if (response.code == 404 && retryTransient404 && method == "GET" && notFoundRetries < 2) {
                notFoundRetries++
                delayMillis(if (notFoundRetries == 1) 1_000L else 3_000L)
                continue
            }

            return response
        }
    }

    private fun <T> decodeBody(response: ApiResponse, decode: (String) -> T): T {
        if (response.code in 200..299) {
            if (response.contentType?.contains("text/html", ignoreCase = true) == true) {
                throw ApiError.Blocked(response.code)
            }
            return try {
                decode(response.body)
            } catch (e: ApiError) {
                throw e
            } catch (e: Exception) {
                throw ApiError.Parse(e)
            }
        }

        throw when (response.code) {
            400 -> ApiError.BadRequest(serverMessage(response.body))
            401 -> ApiError.Unauthorized
            403 -> if (isPlainUnauthorized(response.body) ||
                response.contentType?.contains("text/html", ignoreCase = true) == true
            ) {
                ApiError.Unauthorized
            } else {
                ApiError.Blocked(403)
            }
            404 -> ApiError.NotFound
            405 -> ApiError.EndpointGone(response.code.toString())
            429 -> ApiError.RateLimited(parseRetryAfter(response.header("Retry-After")))
            in 500..599 -> ApiError.Http(response.code)
            else -> ApiError.Http(response.code)
        }
    }

    private fun serverMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun isPlainUnauthorized(body: String): Boolean =
        body.trim().startsWith("Unauthorized", ignoreCase = true)

    private fun parseRetryAfter(value: String?): Long? =
        value?.trim()?.toLongOrNull()?.coerceAtLeast(0)

    private class UserAgentInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .header("User-Agent", UserAgent.VALUE)
                .build()
            return chain.proceed(request)
        }
    }

    private class RateLimitInterceptor(private val limiter: RateLimiter) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            limiter.awaitSlot()
            return chain.proceed(chain.request())
        }
    }

    private class AuthInterceptor(private val provider: () -> TokenProvider?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val requiresAuth = request.tag(AuthRequired::class.java) != null
            if (!requiresAuth) return chain.proceed(request)
            val token = provider()?.cachedToken()
            if (token.isNullOrBlank()) return chain.proceed(request)
            return chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build(),
            )
        }
    }

    private class RequestLogInterceptor(private val log: RequestLog) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val path = chain.request().url.encodedPath
            return try {
                chain.proceed(chain.request()).also { log.record(path, it.code) }
            } catch (e: IOException) {
                log.record(path, 0)
                throw e
            }
        }
    }
}
