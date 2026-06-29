package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaceMinimal(
    val id: Int,
    val name: String = "",
    val address: String = "",
)

@Serializable
data class ProgramMinimal(
    val id: Int,
    val name: String = "",
)

/** GET events/ + events/{id}/ — athlete subset of Event (other fields ignored). */
@Serializable
data class EventDto(
    val id: Int,
    val name: String = "",
    val goal: String? = null,
    val location: String = "",
    val equipment: String = "",
    val date: String? = null,
    @SerialName("hour_start") val hourStart: String? = null,
    @SerialName("hour_end") val hourEnd: String? = null,
    val total: Long = 0,
    val place: PlaceMinimal? = null,
    @SerialName("refer_program") val referProgram: ProgramMinimal? = null,
    @SerialName("vis_distance") val visDistance: String = "never",
    @SerialName("vis_goal") val visGoal: String = "never",
    @SerialName("vis_rounds") val visRounds: String = "never",
    val debrief: String = "",
)

@Serializable
data class PaginatedEventList(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<EventDto> = emptyList(),
)

@Serializable
data class RsvpCounts(
    val going: Int = 0,
    val maybe: Int = 0,
    @SerialName("not_going") val notGoing: Int = 0,
    @SerialName("no_response") val noResponse: Int = 0,
)

@Serializable
data class RsvpSummary(
    val counts: RsvpCounts = RsvpCounts(),
    @SerialName("total_members") val totalMembers: Int = 0,
    @SerialName("my_status") val myStatus: String? = null,
)

/** PUT events/{id}/rsvp/ — status in going|maybe|not_going. */
@Serializable
data class RsvpUpsertRequest(val status: String)

/** PUT events/{id}/roti/ → my_score echoed back (average/distribution managers-only, ignored). */
@Serializable
data class RotiSummary(
    val average: Double? = null,
    val count: Int = 0,
    @SerialName("my_score") val myScore: Int? = null,
)

@Serializable
data class RotiUpsertRequest(val score: Int)
