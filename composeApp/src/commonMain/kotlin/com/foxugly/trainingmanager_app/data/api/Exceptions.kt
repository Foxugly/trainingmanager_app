package com.foxugly.trainingmanager_app.data.api

class ApiException(
    val statusCode: Int,
    operation: String,
    responseBody: String,
) : Exception(
    buildString {
        append("API $operation failed with HTTP $statusCode")
        if (responseBody.isNotBlank()) {
            append(": ")
            append(responseBody)
        }
    }
)

/** Transport failure kind, so the UI can show a localized message per case. */
enum class NetworkErrorKind { OFFLINE, TIMEOUT }

/** A transport-level failure (offline, DNS, timeout) — distinct from an HTTP
 * status error ([ApiException]) so the UI can show "check your connection". */
class NetworkException(
    val kind: NetworkErrorKind,
    message: String,
    cause: Throwable,
) : Exception(message, cause)

class ResponseDecodingException(
    val statusCode: Int,
    operation: String,
    responseBody: String,
    cause: Throwable,
) : Exception(
    buildString {
        append("API ")
        append(operation)
        append(" returned an unexpected response")
        if (responseBody.isNotBlank()) {
            append(": ")
            append(responseBody)
        }
    },
    cause,
)
