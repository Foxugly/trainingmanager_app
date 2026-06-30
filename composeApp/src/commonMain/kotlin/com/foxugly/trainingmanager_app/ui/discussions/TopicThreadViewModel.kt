package com.foxugly.trainingmanager_app.ui.discussions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.TopicMessage
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class TopicThreadViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var messages by mutableStateOf<List<TopicMessage>>(emptyList())
        private set
    var myUserId by mutableStateOf<Int?>(null)
        private set

    var composeText by mutableStateOf("")
    var isSending by mutableStateOf(false)
        private set
    var sendError by mutableStateOf<String?>(null)
        private set

    fun isMine(message: TopicMessage): Boolean = myUserId != null && message.author.id == myUserId

    suspend fun load(teamId: Int, topicId: Int) {
        isLoading = true
        error = null
        authRepository.getCurrentUser().onSuccess { myUserId = it.id }
        authRepository.listMessages(teamId, topicId).fold(
            onSuccess = { messages = it.results },
            onFailure = { error = strings.messagesLoadFailed },
        )
        isLoading = false
    }

    suspend fun send(teamId: Int, topicId: Int) {
        val text = composeText.trim()
        if (isSending || text.isBlank()) return
        isSending = true
        sendError = null
        authRepository.postMessage(teamId, topicId, text).fold(
            onSuccess = { messages = messages + it; composeText = "" },
            onFailure = { sendError = strings.postFailed },
        )
        isSending = false
    }

    suspend fun delete(teamId: Int, topicId: Int, messageId: Int) {
        authRepository.deleteMessage(teamId, topicId, messageId).onSuccess {
            messages = messages.filterNot { it.id == messageId }
        }
    }
}
