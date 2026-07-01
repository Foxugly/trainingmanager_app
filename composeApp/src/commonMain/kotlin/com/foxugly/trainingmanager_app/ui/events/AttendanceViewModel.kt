package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.AttendanceRequest
import com.foxugly.trainingmanager_app.api.generated.models.AttendanceStatus
import com.foxugly.trainingmanager_app.api.generated.models.PatchedAttendanceRequest
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/** One roster member with their current attendance status (if any). */
data class AttendanceRow(
    val memberId: Int,
    val memberName: String,
    val statusId: Int?,
    val recordId: Int?,
)

/**
 * Manager attendance management for an event. The backend allows attendance
 * writes to team owners/managers only (403 otherwise), so this screen is gated
 * to canManage. Shows every roster member with an editable status.
 */
class AttendanceViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var eventId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var statuses by mutableStateOf<List<AttendanceStatus>>(emptyList())
        private set
    var rows by mutableStateOf<List<AttendanceRow>>(emptyList())
        private set

    suspend fun load(eventId: Int) {
        this.eventId = eventId
        isLoading = true
        loadError = null
        val event = authRepository.getEvent(eventId).getOrElse {
            loadError = strings.eventLoadFailed
            isLoading = false
            return
        }
        val teamId = event.teamId
        val members = authRepository.listMembers().getOrNull()?.results
            ?.filter { it.teams.contains(teamId) }
            .orEmpty()
        statuses = authRepository.listAttendanceStatuses().getOrNull()?.results
            ?.filter { it.isActive }?.sortedBy { it.order }
            .orEmpty()
        val records = authRepository.listAttendance(eventId).getOrNull()?.results.orEmpty()
        rows = members.map { m ->
            val rec = records.firstOrNull { it.member == m.id }
            AttendanceRow(
                memberId = m.id,
                memberName = m.fullname.ifBlank {
                    listOf(m.firstname, m.lastname).filter { it.isNotBlank() }.joinToString(" ")
                }.ifBlank { "#${m.id}" },
                statusId = rec?.status,
                recordId = rec?.id,
            )
        }.sortedBy { it.memberName }
        isLoading = false
    }

    suspend fun setStatus(memberId: Int, statusId: Int) {
        val row = rows.firstOrNull { it.memberId == memberId } ?: return
        isSaving = true
        actionError = null
        val result = if (row.recordId != null) {
            authRepository.updateAttendance(eventId, row.recordId, PatchedAttendanceRequest(status = statusId))
        } else {
            authRepository.createAttendance(eventId, AttendanceRequest(member = memberId, status = statusId))
        }
        result.fold(
            onSuccess = { reloadRecords() },
            onFailure = { actionError = strings.attendanceSaveFailed },
        )
        isSaving = false
    }

    private suspend fun reloadRecords() {
        val records = authRepository.listAttendance(eventId).getOrNull()?.results ?: return
        rows = rows.map { r ->
            val rec = records.firstOrNull { it.member == r.memberId }
            r.copy(statusId = rec?.status, recordId = rec?.id)
        }
    }
}
