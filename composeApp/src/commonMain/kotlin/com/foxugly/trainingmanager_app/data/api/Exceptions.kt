package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Backend error code for a rejected/missing Cloudflare Turnstile token. */
const val CAPTCHA_FAILED_CODE = "captcha_failed"

/**
 * An HTTP error status. The body is kept in [responseBody] for callers that
 * match on the backend's error code, but it is deliberately NOT part of the
 * exception message: messages travel to logcat and Sentry (audit A-M1).
 */
class ApiException(
    val statusCode: Int,
    operation: String,
    val responseBody: String,
) : Exception("API $operation failed with HTTP $statusCode") {
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

/**
 * A 2xx body that did not match the expected schema. The body itself is never
 * kept: on auth endpoints it would hold JWTs, and the message ends up in logcat
 * and Sentry (audit A-M1). The cause names the offending field, not its value.
 */
class ResponseDecodingException(
    val statusCode: Int,
    operation: String,
    cause: Throwable,
) : Exception("API $operation returned an unexpected response (HTTP $statusCode)", cause)
