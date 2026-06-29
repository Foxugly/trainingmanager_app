package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.EventDto
import com.foxugly.trainingmanager_app.data.api.RsvpSummary
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EventDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var event by mutableStateOf<EventDto?>(null)
        private set
    var rsvp by mutableStateOf<RsvpSummary?>(null)
        private set
    var rsvpError by mutableStateOf<String?>(null)
        private set
    var isSavingRsvp by mutableStateOf(false)
        private set
    var rotiScore by mutableStateOf<Int?>(null)
        private set
    var rotiError by mutableStateOf<String?>(null)
        private set
    var isSavingRoti by mutableStateOf(false)
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
        authRepository.getEvent(id).fold(
            onSuccess = { e ->
                event = e
                isPast = isEventPast(e.date, today)
            },
            onFailure = { error = strings.eventLoadFailed },
        )
        // RSVP is best-effort (a member may not be able to read it on some teams).
        authRepository.getRsvp(id).onSuccess { rsvp = it }
        isLoading = false
    }

    suspend fun setRsvp(id: Int, status: String) {
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
            onSuccess = { rotiScore = it.myScore ?: score },
            onFailure = { rotiError = strings.rotiFailed },
        )
        isSavingRoti = false
    }
}
