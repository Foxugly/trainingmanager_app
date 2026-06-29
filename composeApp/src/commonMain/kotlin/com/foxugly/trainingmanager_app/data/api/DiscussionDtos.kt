package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: Int,
    val title: String = "",
    val audience: String? = null, // team | coaches
    @SerialName("allow_athlete_replies") val allowAthleteReplies: Boolean = false,
    val author: CustomUserPublic? = null,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TopicMessage(
    val id: Int,
    val content: String = "",
    val author: CustomUserPublic? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PaginatedTopicList(
    val count: Int = 0,
    val results: List<Topic> = emptyList(),
)

@Serializable
data class PaginatedTopicMessageList(
    val count: Int = 0,
    val results: List<TopicMessage> = emptyList(),
)

@Serializable
data class TopicMessageRequest(val content: String)
