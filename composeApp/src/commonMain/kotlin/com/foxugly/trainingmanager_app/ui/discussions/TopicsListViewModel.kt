package com.foxugly.trainingmanager_app.ui.discussions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.AudienceEnum
import com.foxugly.trainingmanager_app.api.generated.models.Topic
import com.foxugly.trainingmanager_app.api.generated.models.TopicCreationEnum
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.ui.teams.isManagedBy
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TopicsListViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    private var teamId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var topics by mutableStateOf<List<Topic>>(emptyList())
        private set
    var myUserId by mutableStateOf<Int?>(null)
        private set

    /** Owner/manager of this team — gates topic deletion of others' topics and reveals coach channels. */
    var canManage by mutableStateOf(false)
        private set

    /** Whether the caller may create a topic, per the team's topic_creation config. */
    var canCreateTopic by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    suspend fun load(teamId: Int) {
        this.teamId = teamId
        isLoading = true
        error = null
        coroutineScope {
            val teamDeferred = async { authRepository.getTeam(teamId) }
            val meDeferred = async { authRepository.getCurrentUser() }
            val topicsDeferred = async { authRepository.listTopics(teamId) }
            val team = teamDeferred.await().getOrNull()
            val myId = meDeferred.await().getOrNull()?.id
            myUserId = myId
            canManage = team?.isManagedBy(myId) == true
            canCreateTopic = when (team?.topicCreation) {
                TopicCreationEnum.OWNER -> team.owner.id == myId
                TopicCreationEnum.COACHES -> canManage
                else -> true // MEMBERS (or unset): any team member may create
            }
            topicsDeferred.await().fold(
                // Managers see every channel; athletes only whole-team topics.
                onSuccess = { topics = visible(it.results) },
                onFailure = { error = strings.topicsLoadFailed },
            )
        }
        isLoading = false
    }

    /** A topic can be removed by its author or by a team manager. */
    fun canDelete(topic: Topic): Boolean =
        myUserId != null && (topic.author.id == myUserId || canManage)

    suspend fun addTopic(title: String) {
        if (title.isBlank()) return
        isSaving = true
        actionError = null
        authRepository.createTopic(teamId, title).fold(
            onSuccess = { authRepository.listTopics(teamId).onSuccess { topics = visible(it.results) } },
            onFailure = { actionError = strings.topicSaveFailed },
        )
        isSaving = false
    }

    suspend fun deleteTopic(topicId: Int) {
        actionError = null
        authRepository.deleteTopic(teamId, topicId).fold(
            onSuccess = { topics = topics.filterNot { it.id == topicId } },
            onFailure = { actionError = strings.topicDeleteFailed },
        )
    }

    private fun visible(all: List<Topic>): List<Topic> =
        if (canManage) all else all.filter { it.audience == AudienceEnum.TEAM }
}
