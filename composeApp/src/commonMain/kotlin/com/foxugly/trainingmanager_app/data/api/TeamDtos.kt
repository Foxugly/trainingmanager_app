package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sport(val id: Int, val name: String = "")

@Serializable
data class CustomUserPublic(
    val id: Int,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
)

/** GET members/ — scoped to the caller's teams; filter client-side by [teams]. */
@Serializable
data class Member(
    val id: Int,
    val firstname: String = "",
    val lastname: String = "",
    val fullname: String = "",
    val teams: List<Int> = emptyList(),
)

@Serializable
data class PaginatedMemberList(
    val count: Int = 0,
    val results: List<Member> = emptyList(),
)

/** GET teams/{id}/ — athlete-visible subset of Team. */
@Serializable
data class TeamDto(
    val id: Int,
    val name: String = "",
    val sport: Sport? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val owner: CustomUserPublic? = null,
    val managers: List<CustomUserPublic> = emptyList(),
)
