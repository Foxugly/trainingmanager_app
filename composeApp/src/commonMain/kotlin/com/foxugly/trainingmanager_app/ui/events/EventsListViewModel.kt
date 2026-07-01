package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Date window applied to the events list. Upcoming is the default — a coach or
 *  athlete cares first about what's next; Past/All widen it on demand. */
enum class EventsFilter { UPCOMING, PAST, ALL }

class EventsListViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var filter by mutableStateOf(EventsFilter.UPCOMING)
        private set

    /** True if the caller manages any team — gates the "+" create affordance. */
    var canCreate by mutableStateOf(false)
        private set
    private var canCreateChecked = false

    @OptIn(ExperimentalTime::class)
    suspend fun load() {
        isLoading = true
        error = null
        if (!canCreateChecked) {
            canCreateChecked = true
            canCreate = authRepository.getDashboard().getOrNull()?.coachTeams?.isNotEmpty() == true
        }
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val result = when (filter) {
            EventsFilter.UPCOMING -> authRepository.listEvents(dateGte = today.toString())
            EventsFilter.PAST -> authRepository.listEvents(dateLte = today.minus(DatePeriod(days = 1)).toString())
            EventsFilter.ALL -> authRepository.listEvents()
        }
        result.fold(
            onSuccess = { events = it.results },
            onFailure = { error = strings.eventsLoadFailed },
        )
        isLoading = false
    }

    suspend fun setFilter(newFilter: EventsFilter) {
        if (newFilter == filter) return
        filter = newFilter
        load()
    }
}
