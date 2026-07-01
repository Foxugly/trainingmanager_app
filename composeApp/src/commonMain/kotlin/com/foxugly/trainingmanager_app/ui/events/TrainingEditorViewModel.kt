package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.EnergySegment
import com.foxugly.trainingmanager_app.api.generated.models.EventRoundDetail
import com.foxugly.trainingmanager_app.api.generated.models.ExerciseRequest
import com.foxugly.trainingmanager_app.api.generated.models.GenerateTrainingRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.api.generated.models.Modality
import com.foxugly.trainingmanager_app.api.generated.models.PatchedExerciseRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedRoundRequest
import com.foxugly.trainingmanager_app.api.generated.models.RoundRequest
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * Edit an event's training: AI generation, clear, and manual round/exercise
 * editing. New rounds take the event's sport; exercises pick a modality (of that
 * sport) and an energy segment. The server authorizes everything to managers
 * only (403).
 */
class TrainingEditorViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
    private val language: LanguageEnum = LanguageEnum.FR,
) {
    private var eventId: Int = 0
    private var eventSportId: Int = 0

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
    var isMutating by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var generatedOk by mutableStateOf(false)
        private set

    // Reference data for the exercise pickers.
    var modalities by mutableStateOf<List<Modality>>(emptyList())
        private set
    var energySegments by mutableStateOf<List<EnergySegment>>(emptyList())
        private set

    val hasTraining: Boolean get() = rounds.isNotEmpty()
    val busy: Boolean get() = isGenerating || isClearing || isMutating

    suspend fun load(eventId: Int) {
        this.eventId = eventId
        isLoading = true
        loadError = null
        authRepository.getEvent(eventId).fold(
            onSuccess = { ev ->
                eventSportId = ev.sport.id
                rounds = ev.roundsDetail.sortedBy { r -> r.order }
            },
            onFailure = { loadError = strings.eventLoadFailed },
        )
        // Reference data is best-effort; empty lists just disable the pickers.
        if (eventSportId != 0) {
            modalities = authRepository.listModalities(eventSportId).getOrNull()?.results?.filter { it.isActive }.orEmpty()
        }
        energySegments = authRepository.listEnergySegments().getOrNull()?.results?.filter { it.isActive }.orEmpty()
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

    suspend fun addRound() = mutate {
        authRepository.createRound(
            RoundRequest(
                sportId = eventSportId,
                language = language,
                eventId = eventId,
                order = (rounds.size + 1).toLong(),
                count = 1,
            ),
        )
    }

    suspend fun setRoundCount(roundId: Int, count: Int) = mutate {
        authRepository.updateRound(roundId, PatchedRoundRequest(count = count.toLong().coerceAtLeast(1)))
    }

    suspend fun removeRound(roundId: Int) = mutate {
        authRepository.deleteRound(roundId)
    }

    suspend fun addExercise(
        roundId: Int,
        modalityId: Int,
        energysegmentId: Int,
        repetition: Long?,
        distance: Long?,
        notes: String?,
    ) = mutate {
        val order = (rounds.firstOrNull { it.id == roundId }?.exercises?.size ?: 0) + 1
        authRepository.createExercise(
            ExerciseRequest(
                roundId = roundId,
                language = language,
                modalityId = modalityId,
                energysegmentId = energysegmentId,
                order = order.toLong(),
                repetition = repetition,
                distance = distance,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    suspend fun updateExercise(
        exerciseId: Int,
        modalityId: Int,
        energysegmentId: Int,
        repetition: Long?,
        distance: Long?,
        notes: String?,
    ) = mutate {
        authRepository.updateExercise(
            exerciseId,
            PatchedExerciseRequest(
                modalityId = modalityId,
                energysegmentId = energysegmentId,
                repetition = repetition,
                distance = distance,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    suspend fun removeExercise(exerciseId: Int) = mutate {
        authRepository.deleteExercise(exerciseId)
    }

    private suspend fun mutate(block: suspend () -> Result<*>) {
        isMutating = true
        actionError = null
        generatedOk = false
        block().fold(
            onSuccess = { reload() },
            onFailure = { actionError = strings.trainingSaveFailed },
        )
        isMutating = false
    }

    private suspend fun reload() {
        authRepository.getEvent(eventId).getOrNull()?.let { ev ->
            rounds = ev.roundsDetail.sortedBy { it.order }
        }
    }
}
