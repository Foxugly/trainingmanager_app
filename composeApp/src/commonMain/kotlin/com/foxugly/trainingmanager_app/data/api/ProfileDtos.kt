package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PATCH me/ — only non-null fields are sent (explicitNulls=false). */
@Serializable
data class PatchMeBody(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val language: String? = null,
    @SerialName("weekly_recap_opt_in") val weeklyRecapOptIn: Boolean? = null,
    @SerialName("digest_email") val digestEmail: Boolean? = null,
)

/** POST auth/password/change/ */
@Serializable
data class PasswordChangeBody(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
