package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: Int,
    @SerialName("target_type") val targetType: String? = null,
    @SerialName("target_id") val targetId: Int = 0,
    val filename: String = "",
    @SerialName("content_type_mime") val contentTypeMime: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val status: String = "pending", // pending | ready
)

@Serializable
data class PaginatedAttachmentList(
    val count: Int = 0,
    val results: List<Attachment> = emptyList(),
)

/** GET attachments/{id}/download/ — short-lived presigned URL. */
@Serializable
data class AttachmentDownloadResponse(val url: String)
