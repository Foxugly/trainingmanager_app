package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Attachment
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.ui.teams.isManagedBy
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EventDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
    private val openUrl: (String) -> Unit = {},
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var event by mutableStateOf<Event?>(null)
        private set
    var rsvp by mutableStateOf<RsvpSummary?>(null)
        private set
    var rsvpError by mutableStateOf<String?>(null)
        private set
    var isSavingRsvp by mutableStateOf(false)
        private set
    var rotiSummary by mutableStateOf<com.foxugly.trainingmanager_app.api.generated.models.RotiSummary?>(null)
        private set
    var rotiScore by mutableStateOf<Int?>(null)
        private set
    var rotiError by mutableStateOf<String?>(null)
        private set
    var isSavingRoti by mutableStateOf(false)
        private set
    var attachments by mutableStateOf<List<Attachment>>(emptyList())
        private set
    var attachmentError by mutableStateOf<String?>(null)
        private set
    var isUploadingAttachment by mutableStateOf(false)
        private set

    /** True if the caller manages this event's team — gates edit/delete. */
    var canManage by mutableStateOf(false)
        private set

    private var isPast by mutableStateOf(false)

    val showDistance: Boolean get() = event?.let { fieldVisible(it.visDistance, isPast) } ?: false
    val showGoal: Boolean get() = event?.let { fieldVisible(it.visGoal, isPast) } ?: false
    val showRounds: Boolean get() = event?.let { fieldVisible(it.visRounds, isPast) } ?: false

    @OptIn(ExperimentalTime::class)
    suspend fun load(id: Int) {
        isLoading = true
        error = null
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        // getEvent, getRsvp and listEventAttachments all depend only on `id`; run concurrently.
        coroutineScope {
            val eventDeferred = async { authRepository.getEvent(id) }
            val rsvpDeferred = async { authRepository.getRsvp(id) }
            val attachmentsDeferred = async { authRepository.listEventAttachments(id) }
            val meDeferred = async { authRepository.getCurrentUser() }
            val rotiDeferred = async { authRepository.getRotiSummary(id) }
            eventDeferred.await().fold(
                onSuccess = { e ->
                    event = e
                    isPast = isEventPast(e.date, today)
                },
                onFailure = { error = strings.eventLoadFailed },
            )
            // RSVP is best-effort (a member may not be able to read it on some teams).
            rsvpDeferred.await().onSuccess { rsvp = it }
            // ROTI summary (aggregate difficulty) — best-effort; also carries my score.
            rotiDeferred.await().onSuccess { rotiSummary = it; rotiScore = it.myScore }
            // Attachments are best-effort too.
            attachmentsDeferred.await().onSuccess { attachments = it.results }
            // Coach gating: do I manage this event's team? (best-effort)
            event?.let { e ->
                val meId = meDeferred.await().getOrNull()?.id
                canManage = authRepository.getTeam(e.teamId).getOrNull()?.isManagedBy(meId) == true
            }
        }
        isLoading = false
    }

    /** Open the location in an external maps app (a GPS link, since places carry
     *  a free-text address, not coordinates). */
    fun openMap(query: String) {
        if (query.isBlank()) return
        openUrl("https://www.google.com/maps/search/?api=1&query=${query.encodeURLQueryComponent()}")
    }

    suspend fun downloadAttachment(attachmentId: Int) {
        attachmentError = null
        authRepository.attachmentDownloadUrl(attachmentId).fold(
            onSuccess = { openUrl(it.url) },
            onFailure = { attachmentError = strings.downloadFailed },
        )
    }

    suspend fun uploadAttachment(filename: String, contentType: String, bytes: ByteArray) {
        val id = event?.id ?: return
        isUploadingAttachment = true
        attachmentError = null
        authRepository.uploadEventAttachment(id, filename, contentType, bytes).fold(
            onSuccess = { authRepository.listEventAttachments(id).onSuccess { attachments = it.results } },
            onFailure = { attachmentError = strings.attachmentUploadFailed },
        )
        isUploadingAttachment = false
    }

    suspend fun deleteAttachment(attachmentId: Int) {
        attachmentError = null
        authRepository.deleteAttachment(attachmentId).fold(
            onSuccess = { attachments = attachments.filterNot { it.id == attachmentId } },
            onFailure = { attachmentError = strings.attachmentDeleteFailed },
        )
    }

    var isSavingNotes by mutableStateOf(false)
        private set
    var notesError by mutableStateOf<String?>(null)
        private set

    suspend fun saveNotesVisibility(
        debrief: String,
        visDistance: com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode,
        visGoal: com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode,
        visRounds: com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode,
    ) {
        val id = event?.id ?: return
        isSavingNotes = true
        notesError = null
        val body = com.foxugly.trainingmanager_app.api.generated.models.PatchedEventRequest(
            debrief = debrief,
            visDistance = visDistance,
            visGoal = visGoal,
            visRounds = visRounds,
        )
        authRepository.updateEvent(id, body).fold(
            onSuccess = { event = it },
            onFailure = { notesError = strings.eventSaveFailed },
        )
        isSavingNotes = false
    }

    suspend fun setRsvp(id: Int, status: RsvpStatusEnum) {
        if (isSavingRsvp) return
        isSavingRsvp = true
        rsvpError = null
        authRepository.setRsvp(id, status).fold(
            onSuccess = { rsvp = it },
            onFailure = { t ->
                rsvpError = if (t is ApiException && t.statusCode == 403) strings.rsvpDisabled else strings.rsvpFailed
            },
        )
        isSavingRsvp = false
    }

    suspend fun setRoti(id: Int, score: Int) {
        if (isSavingRoti) return
        isSavingRoti = true
        rotiError = null
        authRepository.setRoti(id, score).fold(
            onSuccess = { rotiScore = it.myScore ?: score; rotiSummary = it },
            onFailure = { rotiError = strings.rotiFailed },
        )
        isSavingRoti = false
    }
}
