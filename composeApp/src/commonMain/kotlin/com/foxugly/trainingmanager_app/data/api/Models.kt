package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Minimal hand-written auth DTOs for S1a. The full API surface is generated from
// the OpenAPI schema in a later S1 plan and supersedes anything overlapping here.

/** POST auth/token/ — SimpleJWT obtain-pair, plus the "stay logged in" flag the
 * backend maps to a 7d vs 30d refresh TTL. */
@Serializable
data class TokenObtainRequest(
    val email: String,
    val password: String,
    val remember: Boolean = false,
)

/** Response of POST auth/token/. */
@Serializable
data class TokenPair(
    val access: String,
    val refresh: String,
)

@Serializable
data class RefreshRequest(
    val refresh: String,
)

@Serializable
data class RefreshResponse(
    val access: String,
    // The backend rotates + blacklists refresh tokens, so each refresh returns a
    // NEW refresh token. Persist it, else the next refresh presents a blacklisted
    // token and the user is ejected. Nullable so a non-rotating backend still
    // deserializes.
    val refresh: String? = null,
)

/** GET me/. */
@Serializable
data class UserProfile(
    val id: Int,
    val email: String,
    @SerialName("email_confirmed") val emailConfirmed: Boolean? = null,
    val language: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("weekly_recap_opt_in") val weeklyRecapOptIn: Boolean? = null,
    @SerialName("digest_email") val digestEmail: Boolean? = null,
)
