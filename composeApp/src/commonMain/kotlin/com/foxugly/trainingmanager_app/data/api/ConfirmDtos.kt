package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST auth/email/confirm/ — confirm registration via the e-mailed key. */
@Serializable
data class EmailConfirmBody(val key: String)

/** POST auth/password/reset/confirm/ — set a new password via the e-mailed key. */
@Serializable
data class PasswordResetConfirmBody(
    val key: String,
    @SerialName("new_password") val newPassword: String,
)
