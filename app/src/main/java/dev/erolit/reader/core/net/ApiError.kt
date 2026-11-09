package dev.erolit.reader.core.net

import java.io.IOException

/**
 * Typed failure surface for every network call. UI code must map these to short,
 * non-leaky user messages; raw bodies can contain site copy and are never displayed.
 */
sealed class ApiError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val code: Int) : ApiError("HTTP $code")
    data object Unauthorized : ApiError("Unauthorized")
    data object SessionExpired : ApiError("Session expired")
    data object NotFound : ApiError("Not found")
    class BadRequest(val serverMessage: String?) : ApiError(serverMessage ?: "Bad request")
    class RateLimited(val retryAfterSeconds: Long?) : ApiError("Rate limited")
    class Parse(cause: Throwable) : ApiError("Parse error", cause)
    class Network(cause: Throwable) : ApiError("Network error", cause)
    class Blocked(val code: Int) : ApiError("Blocked ($code)")
    class EndpointGone(val path: String) : ApiError("Endpoint gone: $path")
}

fun ApiError.userMessage(): String = when (this) {
    is ApiError.Http -> "The service returned an error ($code). Try again shortly."
    ApiError.Unauthorized -> "Please sign in to continue."
    ApiError.SessionExpired -> "Your session expired. Please sign in again."
    ApiError.NotFound -> "That item was not found. It may have been removed, or the service is busy."
    is ApiError.BadRequest -> "The request was rejected. Check the filters and try again."
    is ApiError.RateLimited -> "The service asked us to slow down. Please wait a moment."
    is ApiError.Parse -> "The service returned an unexpected response."
    is ApiError.Network -> "No connection. Check your network and try again."
    is ApiError.Blocked -> "The service refused this request. Please try again later."
    is ApiError.EndpointGone -> "This part of the service is no longer available."
}

internal fun Throwable.toApiError(): ApiError = when (this) {
    is ApiError -> this
    is IOException -> ApiError.Network(this)
    else -> ApiError.Network(this)
}
