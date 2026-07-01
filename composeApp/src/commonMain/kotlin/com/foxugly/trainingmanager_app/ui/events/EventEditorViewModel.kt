package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.EventRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedEventRequest
import com.foxugly.trainingmanager_app.api.generated.models.Program
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * Create or edit an event. [eventId] null = create. [presetTeamId] pre-selects
 * the team (team-detail entry); when null in create mode the editor offers a
 * picker over the caller's managed teams.
 *
 * Scope: schedule fields only (name, program, date, hours, location, distance).
 * The rich-text objective/equipment stay web-managed to avoid an HTML round-trip;
 * the training itself (rounds/exercises) is edited in the training editor.
 */
class EventEditorViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var eventId: Int? = null

    var isNew by mutableStateOf(true)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    // Editable fields (plain, backend-validated on save).
    var name by mutableStateOf("")
    var date by mutableStateOf("")
    var hourStart by mutableStateOf("")
    var hourEnd by mutableStateOf("")
    var location by mutableStateOf("")
    var distance by mutableStateOf("")

    var selectedTeamId by mutableStateOf<Int?>(null)
        private set
    var selectedProgramId by mutableStateOf<Int?>(null)
        private set

    /** In edit mode the event's program is fixed and shown read-only. */
    var existingProgramName by mutableStateOf("")
        private set

    var teams by mutableStateOf<List<Team>>(emptyList())
        private set
    var programs by mutableStateOf<List<Program>>(emptyList())
        private set

    /** Create needs a team + program; edit keeps its program. Name always required. */
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving &&
            (eventId != null || (selectedTeamId != null && selectedProgramId != null))

    suspend fun load(eventId: Int?, teamId: Int?) {
        this.eventId = eventId
        selectedTeamId = teamId
        isNew = eventId == null
        isLoading = true
        loadError = null
        if (eventId != null) {
            authRepository.getEvent(eventId).fold(
                onSuccess = { ev ->
                    name = ev.name
                    date = ev.date.orEmpty()
                    hourStart = ev.hourStart.orEmpty()
                    hourEnd = ev.hourEnd.orEmpty()
                    location = ev.location.orEmpty()
                    distance = ev.total?.toString().orEmpty()
                    existingProgramName = ev.referProgram.name
                    selectedProgramId = ev.referProgram.id
                },
                onFailure = { loadError = strings.eventsLoadFailed },
            )
        } else if (teamId != null) {
            loadPrograms(teamId)
        } else {
            loadManagedTeams()
        }
        isLoading = false
    }

    suspend fun selectTeam(teamId: Int) {
        if (teamId == selectedTeamId) return
        selectedTeamId = teamId
        selectedProgramId = null
        programs = emptyList()
        loadPrograms(teamId)
    }

    fun selectProgram(id: Int) {
        selectedProgramId = id
    }

    private suspend fun loadManagedTeams() {
        val summary = authRepository.getDashboard().getOrNull() ?: return
        val ids = summary.coachTeams.map { it.teamId }.distinct()
        teams = ids.mapNotNull { authRepository.getTeam(it).getOrNull() }
    }

    private suspend fun loadPrograms(teamId: Int) {
        authRepository.listPrograms(teamId).fold(
            onSuccess = { programs = it.results },
            onFailure = { /* leave empty — save stays disabled until a program is chosen */ },
        )
    }

    suspend fun save(onSaved: (Int) -> Unit) {
        isSaving = true
        saveError = null
        val id = eventId
        val result = if (id == null) {
            authRepository.createEvent(
                EventRequest(
                    name = name.trim(),
                    referProgramId = selectedProgramId!!,
                    date = date.ifBlank { null },
                    hourStart = hourStart.ifBlank { null },
                    hourEnd = hourEnd.ifBlank { null },
                    location = location.ifBlank { null },
                    total = distance.trim().toLongOrNull(),
                ),
            )
        } else {
            authRepository.updateEvent(
                id,
                PatchedEventRequest(
                    name = name.trim(),
                    date = date.ifBlank { null },
                    hourStart = hourStart.ifBlank { null },
                    hourEnd = hourEnd.ifBlank { null },
                    location = location.ifBlank { null },
                    total = distance.trim().toLongOrNull(),
                ),
            )
        }
        result.fold(
            onSuccess = { onSaved(it.id) },
            onFailure = { saveError = strings.eventSaveFailed },
        )
        isSaving = false
    }

    suspend fun delete(onDeleted: () -> Unit) {
        val id = eventId ?: return
        isSaving = true
        saveError = null
        authRepository.deleteEvent(id).fold(
            onSuccess = { onDeleted() },
            onFailure = { saveError = strings.eventDeleteFailed },
        )
        isSaving = false
    }
}
