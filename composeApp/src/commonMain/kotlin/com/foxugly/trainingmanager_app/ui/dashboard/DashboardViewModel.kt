package com.foxugly.trainingmanager_app.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.DashboardSummary
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var summary by mutableStateOf<DashboardSummary?>(null)
        private set

    suspend fun load() {
        isLoading = true
        error = null
        authRepository.getDashboard().fold(
            onSuccess = { summary = it },
            onFailure = { error = strings.dashboardLoadFailed },
        )
        isLoading = false
    }
}
