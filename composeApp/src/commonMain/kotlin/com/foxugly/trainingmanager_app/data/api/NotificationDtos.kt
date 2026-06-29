package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Int,
    val type: String? = null,
    val title: String = "",
    val body: String = "",
    val url: String = "", // frontend deep-link path, e.g. /teams/3
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PaginatedNotificationList(
    val count: Int = 0,
    val results: List<Notification> = emptyList(),
)
