package com.foxugly.trainingmanager_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import org.koin.compose.koinInject

/**
 * Root composable + startup auth-check stub. Runs the bootstrap once, then shows
 * a placeholder per [StartupRoute]. The real screens + Navigation Compose route
 * graph replace the placeholders in later S1 plans.
 */
@Composable
fun App(authRepository: AuthRepository = koinInject()) {
    var route by remember { mutableStateOf(StartupRoute.Loading) }

    LaunchedEffect(authRepository) {
        val hasRefresh = authRepository.hasRefreshToken()
        val refreshed = hasRefresh && authRepository.tryRefresh()
        route = startupRoute(hasRefresh, refreshed)
    }

    TrainingManagerTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (route) {
                StartupRoute.Loading -> CircularProgressIndicator()
                StartupRoute.Authenticated -> Text("TrainingManager — signed in (placeholder home)")
                StartupRoute.Unauthenticated -> Text("TrainingManager — sign in (placeholder login)")
            }
        }
    }
}
