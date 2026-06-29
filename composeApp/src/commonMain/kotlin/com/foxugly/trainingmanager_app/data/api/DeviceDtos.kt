package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST devices/register/ — upsert by push_token, bound to the caller. */
@Serializable
data class DeviceRegisterBody(
    @SerialName("push_token") val pushToken: String,
    val platform: String, // android | ios
    @SerialName("device_name") val deviceName: String = "",
)

/** POST devices/unregister/ — soft-revoke on logout. */
@Serializable
data class DeviceUnregisterBody(
    @SerialName("push_token") val pushToken: String,
)
