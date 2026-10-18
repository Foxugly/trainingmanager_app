package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Backend error code for a rejected/missing Cloudflare Turnstile token. */
const val CAPTCHA_FAILED_CODE = "captcha_failed"

class ApiException(
    val statusCode: Int,
    operation: String,
    val responseBody: String,
) : Exception(
    buildString {
        append("API $operation failed with HTTP $statusCode")
        if (responseBody.isNotBlank()) {
            append(": ")
            append(responseBody)
        }
    }
) {
    /** The backend's normalised `{code, detail}` error code, if the body carries one. */
    val errorCode: String? by lazy {
        runCatching {
            Json.parseToJsonElement(responseBody).jsonObject["code"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}

/** True when the backend rejected the request's Turnstile token. */
val Throwable.isCaptchaFailed: Boolean
    get() = this is ApiException && errorCode == CAPTCHA_FAILED_CODE

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
