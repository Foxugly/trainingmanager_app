package com.foxugly.trainingmanager_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.navigation.HomeRoute
import com.foxugly.trainingmanager_app.navigation.LoginRoute
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.home.HomePlaceholderScreen
import com.foxugly.trainingmanager_app.ui.login.LoginScreen
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import org.koin.compose.koinInject

/**
 * Root composable. Runs the bootstrap once (reuses the pure [startupRoute]); while
 * loading shows a spinner, then mounts the Navigation Compose graph at the resolved
 * start destination. Post-login / logout clear the back stack so Back cannot return
 * to the other auth state.
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
        when (route) {
            StartupRoute.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else -> {
                val navController = rememberNavController()
                val startDestination: Any =
                    if (route == StartupRoute.Authenticated) HomeRoute else LoginRoute
                NavHost(navController = navController, startDestination = startDestination) {
                    composable<LoginRoute> {
                        val loginViewModel: LoginViewModel = koinInject()
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<HomeRoute> {
                        HomePlaceholderScreen(
                            authRepository = authRepository,
                            onLoggedOut = {
                                navController.navigate(LoginRoute) {
                                    popUpTo<HomeRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
