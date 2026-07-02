package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Equipment
import com.foxugly.trainingmanager_app.api.generated.models.InvitationStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.api.generated.models.Member
import com.foxugly.trainingmanager_app.api.generated.models.PatchedTeamRequest
import com.foxugly.trainingmanager_app.api.generated.models.TrainingSlotRequest
import com.foxugly.trainingmanager_app.api.generated.models.WeekdayEnum
import com.foxugly.trainingmanager_app.api.generated.models.Program
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.api.generated.models.TeamInvitation
import com.foxugly.trainingmanager_app.api.generated.models.TrainingSlot
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TeamDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var teamId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var team by mutableStateOf<Team?>(null)
        private set
    var members by mutableStateOf<List<Member>>(emptyList())
        private set
    var programs by mutableStateOf<List<Program>>(emptyList())
        private set
    var invitations by mutableStateOf<List<TeamInvitation>>(emptyList())
        private set
    var slots by mutableStateOf<List<TrainingSlot>>(emptyList())
        private set
    var isSavingProgram by mutableStateOf(false)
        private set
    var programError by mutableStateOf<String?>(null)
        private set
    var isSavingInvite by mutableStateOf(false)
        private set
    var inviteError by mutableStateOf<String?>(null)
        private set

    /** True if the caller owns/manages this team — gates the coach affordances. */
    var canManage by mutableStateOf(false)
        private set

    suspend fun load(id: Int) {
        teamId = id
        isLoading = true
        error = null
        // getTeam, listMembers, me and programs are independent; run them concurrently.
        coroutineScope {
            val teamDeferred = async { authRepository.getTeam(id) }
            val membersDeferred = async { authRepository.listMembers() }
            val meDeferred = async { authRepository.getCurrentUser() }
            val programsDeferred = async { authRepository.listPrograms(id) }
            val invitationsDeferred = async { authRepository.listInvitations() }
            val slotsDeferred = async { authRepository.listTrainingSlots(id) }
            teamDeferred.await().fold(
                onSuccess = { team = it },
                onFailure = { error = strings.teamLoadFailed },
            )
            val meId = meDeferred.await().getOrNull()?.id
            canManage = team?.isManagedBy(meId) == true
            // Roster is best-effort: /members/ is scoped to the caller's teams; filter to this one.
            membersDeferred.await().onSuccess { page ->
                members = page.results.filter { it.teams.contains(id) }
            }
            programsDeferred.await().onSuccess { programs = it.results }
            slotsDeferred.await().onSuccess { slots = it }
            // Invitations: managers only see them; filter to this team + still pending.
            if (canManage) {
                invitationsDeferred.await().onSuccess { page ->
                    invitations = page.results.filter { it.team == id && it.status == InvitationStatusEnum.PENDING }
                }
            }
        }
        isLoading = false
    }

    private suspend fun reloadInvitations() {
        authRepository.listInvitations().onSuccess { page ->
            invitations = page.results.filter { it.team == teamId && it.status == InvitationStatusEnum.PENDING }
        }
    }

    suspend fun invite(email: String, firstname: String, lastname: String) {
        if (email.isBlank()) return
        isSavingInvite = true
        inviteError = null
        authRepository.createInvitation(teamId, email, firstname, lastname).fold(
            onSuccess = { reloadInvitations() },
            onFailure = { inviteError = strings.inviteFailed },
        )
        isSavingInvite = false
    }

    suspend fun cancelInvitation(id: Int) {
        isSavingInvite = true
        inviteError = null
        authRepository.cancelInvitation(id).fold(
            onSuccess = { reloadInvitations() },
            onFailure = { inviteError = strings.inviteFailed },
        )
        isSavingInvite = false
    }

    // --- TT2: team info edit ---
    var isSavingTeam by mutableStateOf(false)
        private set
    var teamError by mutableStateOf<String?>(null)
        private set

    suspend fun saveInfos(name: String, language: LanguageEnum?, isPublic: Boolean, rotiEnabled: Boolean, rsvpEnabled: Boolean) {
        if (name.isBlank()) return
        isSavingTeam = true
        teamError = null
        val body = PatchedTeamRequest(
            name = name.trim(),
            language = language,
            isPublic = isPublic,
            rotiEnabled = rotiEnabled,
            rsvpEnabled = rsvpEnabled,
        )
        authRepository.patchTeam(teamId, body).fold(
            onSuccess = { team = it },
            onFailure = { teamError = strings.teamSaveFailed },
        )
        isSavingTeam = false
    }

    // --- TT4: places & training slots ---
    var placesError by mutableStateOf<String?>(null)
        private set

    private suspend fun reloadTeam() {
        authRepository.getTeam(teamId).onSuccess { team = it }
    }

    private suspend fun reloadSlots() {
        authRepository.listTrainingSlots(teamId).onSuccess { slots = it }
    }

    suspend fun addPlace(name: String, address: String) {
        if (name.isBlank()) return
        placesError = null
        authRepository.createPlace(name, address, teamId).fold(
            onSuccess = { reloadTeam() },
            onFailure = { placesError = strings.placeSaveFailed },
        )
    }

    suspend fun removePlace(id: Int) {
        placesError = null
        authRepository.deletePlace(id).fold(
            onSuccess = { reloadTeam() },
            onFailure = { placesError = strings.placeSaveFailed },
        )
    }

    suspend fun addSlot(weekday: Int, hourStart: String, hourEnd: String, placeId: Int?) {
        placesError = null
        val body = TrainingSlotRequest(
            weekday = WeekdayEnum.decode(weekday) ?: WeekdayEnum._0,
            hourStart = hourStart,
            hourEnd = hourEnd,
            placeId = placeId,
        )
        authRepository.createTrainingSlot(teamId, body).fold(
            onSuccess = { reloadSlots() },
            onFailure = { placesError = strings.placeSaveFailed },
        )
    }

    suspend fun editPlace(id: Int, name: String, address: String) {
        if (name.isBlank()) return
        placesError = null
        authRepository.updatePlace(id, name, address).fold(
            onSuccess = { reloadTeam() },
            onFailure = { placesError = strings.placeSaveFailed },
        )
    }

    suspend fun removeSlot(id: Int) {
        placesError = null
        authRepository.deleteTrainingSlot(teamId, id).fold(
            onSuccess = { reloadSlots() },
            onFailure = { placesError = strings.placeSaveFailed },
        )
    }

    // --- TT5: equipment (pick from the read-only catalog via team.equipment_ids) ---
    var equipmentCatalog by mutableStateOf<List<Equipment>>(emptyList())
        private set
    var equipmentError by mutableStateOf<String?>(null)
        private set

    suspend fun loadEquipmentCatalog() {
        authRepository.listEquipment(sport = team?.sport?.id).onSuccess { equipmentCatalog = it.results }
    }

    suspend fun setEquipment(ids: List<Int>) {
        equipmentError = null
        authRepository.patchTeam(teamId, PatchedTeamRequest(equipmentIds = ids)).fold(
            onSuccess = { team = it },
            onFailure = { equipmentError = strings.equipmentSaveFailed },
        )
    }

    suspend fun addProgram(name: String) {
        if (name.isBlank()) return
        isSavingProgram = true
        programError = null
        authRepository.createProgram(teamId, name).fold(
            onSuccess = { authRepository.listPrograms(teamId).onSuccess { programs = it.results } },
            onFailure = { programError = strings.programSaveFailed },
        )
        isSavingProgram = false
    }
}
