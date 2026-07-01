package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.EventRoundDetail
import com.foxugly.trainingmanager_app.api.generated.models.GenerateTrainingRequestRequest
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * Edit an event's training. v1 = AI generation + clear; the training is displayed
 * read-only (fine-grained round/exercise editing needs sport/modality pickers and
 * is a follow-up). The server authorizes both to managers only (403).
 */
class TrainingEditorViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var eventId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var rounds by mutableStateOf<List<EventRoundDetail>>(emptyList())
        private set
    var additionalPrompt by mutableStateOf("")
    var isGenerating by mutableStateOf(false)
        private set
    var isClearing by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var generatedOk by mutableStateOf(false)
        private set

    val hasTraining: Boolean get() = rounds.isNotEmpty()
    val busy: Boolean get() = isGenerating || isClearing

    suspend fun load(eventId: Int) {
        this.eventId = eventId
        isLoading = true
        loadError = null
        authRepository.getEvent(eventId).fold(
            onSuccess = { rounds = it.roundsDetail.sortedBy { r -> r.order } },
            onFailure = { loadError = strings.eventLoadFailed },
        )
        isLoading = false
    }

    suspend fun generate() {
        isGenerating = true
        actionError = null
        generatedOk = false
        authRepository.generateTraining(
            eventId,
            GenerateTrainingRequestRequest(additionalPrompt = additionalPrompt.ifBlank { null }),
        ).fold(
            onSuccess = {
                generatedOk = true
                additionalPrompt = ""
                reload()
            },
            onFailure = { e ->
                actionError = if ((e as? ApiException)?.statusCode == 409) {
                    strings.trainingConflict
                } else {
                    strings.trainingGenerateFailed
                }
            },
        )
        isGenerating = false
    }

    suspend fun clear() {
        isClearing = true
        actionError = null
        generatedOk = false
        val allDeleted = rounds.all { authRepository.deleteRound(it.id).isSuccess }
        if (allDeleted) reload() else actionError = strings.trainingClearFailed
        isClearing = false
    }

    private suspend fun reload() {
        authRepository.getEvent(eventId).getOrNull()?.let { ev ->
            rounds = ev.roundsDetail.sortedBy { it.order }
        }
    }
}
