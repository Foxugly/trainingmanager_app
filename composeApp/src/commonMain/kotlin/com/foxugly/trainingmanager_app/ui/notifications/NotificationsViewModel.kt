package com.foxugly.trainingmanager_app.ui.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.Notification
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class NotificationsViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notifications by mutableStateOf<List<Notification>>(emptyList())
        private set

    suspend fun load() {
        isLoading = true
        error = null
        authRepository.listNotifications().fold(
            onSuccess = { notifications = it.results },
            onFailure = { error = strings.notificationsLoadFailed },
        )
        isLoading = false
    }

    suspend fun markRead(id: Int) {
        // Optimistic local update; server call is best-effort.
        notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        authRepository.markNotificationRead(id)
    }

    suspend fun markAllRead() {
        notifications = notifications.map { it.copy(isRead = true) }
        authRepository.markAllNotificationsRead()
    }
}
