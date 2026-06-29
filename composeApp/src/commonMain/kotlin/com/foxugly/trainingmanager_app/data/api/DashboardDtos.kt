package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET dashboard/summary/ — athlete-relevant subset. coach_* and the nested
// place/status objects are dropped via ignoreUnknownKeys.

@Serializable
data class DashboardEvent(
    val id: Int,
    val name: String = "",
    val date: String? = null,
    @SerialName("hour_start") val hourStart: String? = null,
    @SerialName("hour_end") val hourEnd: String? = null,
    val location: String = "",
)

@Serializable
data class DashboardEventItem(
    val event: DashboardEvent,
    @SerialName("team_id") val teamId: Int,
    @SerialName("team_name") val teamName: String = "",
    @SerialName("program_name") val programName: String? = null,
)

@Serializable
data class DashboardHistoryItem(
    val event: DashboardEvent,
    @SerialName("team_id") val teamId: Int,
    @SerialName("team_name") val teamName: String = "",
    @SerialName("program_name") val programName: String? = null,
    @SerialName("status_code") val statusCode: String = "",
)

@Serializable
data class DashboardMemberTeam(
    @SerialName("team_id") val teamId: Int,
    @SerialName("members_count") val membersCount: Int = 0,
    @SerialName("my_member_id") val myMemberId: Int? = null,
)

@Serializable
data class DashboardSummary(
    @SerialName("member_teams") val memberTeams: List<DashboardMemberTeam> = emptyList(),
    @SerialName("member_upcoming") val memberUpcoming: List<DashboardEventItem> = emptyList(),
    @SerialName("member_upcoming_total") val memberUpcomingTotal: Int = 0,
    @SerialName("member_attendance_history") val memberAttendanceHistory: List<DashboardHistoryItem> = emptyList(),
)
