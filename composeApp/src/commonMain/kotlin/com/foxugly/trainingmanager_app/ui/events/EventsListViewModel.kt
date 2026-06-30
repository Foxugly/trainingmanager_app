package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

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

    suspend fun load() {
        isLoading = true
        error = null
        authRepository.listEvents().fold(
            onSuccess = { events = it.results },
            onFailure = { error = strings.eventsLoadFailed },
        )
        isLoading = false
    }
}
