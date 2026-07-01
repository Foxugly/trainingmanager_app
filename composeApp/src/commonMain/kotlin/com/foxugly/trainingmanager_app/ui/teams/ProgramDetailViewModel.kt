package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.api.generated.models.Program
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import com.foxugly.trainingmanager_app.ui.events.EventsFilter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class ProgramDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var programId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var program by mutableStateOf<Program?>(null)
        private set
    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var filter by mutableStateOf(EventsFilter.UPCOMING)
        private set

    suspend fun load(id: Int) {
        programId = id
        isLoading = true
        error = null
        authRepository.getProgram(id).fold(
            onSuccess = { program = it },
            onFailure = { error = strings.programLoadFailed },
        )
        loadEvents()
        isLoading = false
    }

    suspend fun setFilter(newFilter: EventsFilter) {
        if (newFilter == filter) return
        filter = newFilter
        loadEvents()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun loadEvents() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val result = when (filter) {
            EventsFilter.UPCOMING -> authRepository.listEvents(dateGte = today.toString(), referProgram = programId)
            EventsFilter.PAST -> authRepository.listEvents(dateLte = today.minus(DatePeriod(days = 1)).toString(), referProgram = programId)
            EventsFilter.ALL -> authRepository.listEvents(referProgram = programId)
        }
        result.onSuccess { events = it.results }
    }
}
