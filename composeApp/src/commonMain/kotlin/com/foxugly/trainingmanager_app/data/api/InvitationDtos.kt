package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET invitations/lookup/{token}/ */
@Serializable
data class ValidateInvitation(
    val email: String,
    @SerialName("team_name") val teamName: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
)

/** POST invitations/lookup/{token}/ */
@Serializable
data class CompleteInvitationBody(val password: String)
