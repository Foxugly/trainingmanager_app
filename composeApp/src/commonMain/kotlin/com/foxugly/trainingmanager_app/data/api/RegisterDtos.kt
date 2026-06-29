package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST auth/register/ — self-signup. `turnstile_token` is the captcha token from
 *  the WebView-hosted Turnstile widget (see backend /turnstile/). */
@Serializable
data class RegisterBody(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val password: String,
    val language: String,
    @SerialName("turnstile_token") val turnstileToken: String,
)

/** POST auth/password/reset/ — always-200 (no enumeration). Carries the captcha token. */
@Serializable
data class PasswordResetRequestBody(
    val email: String,
    @SerialName("turnstile_token") val turnstileToken: String,
)
