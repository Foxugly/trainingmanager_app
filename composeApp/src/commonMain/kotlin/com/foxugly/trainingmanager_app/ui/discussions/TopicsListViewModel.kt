package com.foxugly.trainingmanager_app.ui.discussions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.AudienceEnum
import com.foxugly.trainingmanager_app.api.generated.models.Topic
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class TopicsListViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var topics by mutableStateOf<List<Topic>>(emptyList())
        private set

    suspend fun load(teamId: Int) {
        isLoading = true
        error = null
        authRepository.listTopics(teamId).fold(
            // Athletes only see whole-team topics; coaches-only channels are hidden.
            onSuccess = { topics = it.results.filter { t -> t.audience == AudienceEnum.TEAM } },
            onFailure = { error = strings.topicsLoadFailed },
        )
        isLoading = false
    }
}
