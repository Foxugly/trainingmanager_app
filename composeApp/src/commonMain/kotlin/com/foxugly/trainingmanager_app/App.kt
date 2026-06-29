package com.foxugly.trainingmanager_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.navigation.toRoute
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.LanguageService
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.navigation.DeepLinkTarget
import com.foxugly.trainingmanager_app.navigation.EmailConfirmRoute
import com.foxugly.trainingmanager_app.navigation.EventDetailRoute
import com.foxugly.trainingmanager_app.navigation.EventsListRoute
import com.foxugly.trainingmanager_app.navigation.HomeRoute
import com.foxugly.trainingmanager_app.navigation.InvitationRoute
import com.foxugly.trainingmanager_app.navigation.ChangePasswordRoute
import com.foxugly.trainingmanager_app.navigation.LoginRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkExchangeRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkRequestRoute
import com.foxugly.trainingmanager_app.navigation.ProfileRoute
import com.foxugly.trainingmanager_app.navigation.ResetPasswordRoute
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmScreen
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmViewModel
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordScreen
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordViewModel
import com.foxugly.trainingmanager_app.ui.dashboard.DashboardScreen
import com.foxugly.trainingmanager_app.ui.dashboard.DashboardViewModel
import com.foxugly.trainingmanager_app.ui.events.EventDetailScreen
import com.foxugly.trainingmanager_app.ui.events.EventDetailViewModel
import com.foxugly.trainingmanager_app.ui.events.EventsListScreen
import com.foxugly.trainingmanager_app.ui.events.EventsListViewModel
import com.foxugly.trainingmanager_app.ui.invitation.InvitationScreen
import com.foxugly.trainingmanager_app.ui.invitation.InvitationViewModel
import com.foxugly.trainingmanager_app.ui.login.LoginScreen
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
import com.foxugly.trainingmanager_app.ui.profile.ChangePasswordScreen
import com.foxugly.trainingmanager_app.ui.profile.ChangePasswordViewModel
import com.foxugly.trainingmanager_app.ui.profile.ProfileScreen
import com.foxugly.trainingmanager_app.ui.profile.ProfileViewModel
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import org.koin.compose.koinInject

@Composable
fun App(
    authRepository: AuthRepository = koinInject(),
    languageService: LanguageService = koinInject(),
    deepLink: DeepLinkTarget? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    var route by remember { mutableStateOf(StartupRoute.Loading) }
    LaunchedEffect(authRepository) {
        val hasRefresh = authRepository.hasRefreshToken()
        val refreshed = hasRefresh && authRepository.tryRefresh()
        val resolved = startupRoute(hasRefresh, refreshed)
        // Initialize the UI language from the signed-in user's preference.
        if (resolved == StartupRoute.Authenticated) {
            authRepository.getCurrentUser().getOrNull()?.language?.let { languageService.setActive(it) }
        }
        route = resolved
    }

    TrainingManagerTheme {
        CompositionLocalProvider(LocalStrings provides languageService.strings) {
        when (route) {
            StartupRoute.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else -> {
                val navController = rememberNavController()
                val startDestination: Any =
                    if (route == StartupRoute.Authenticated) HomeRoute else LoginRoute

                // Route an incoming deep link once the graph is live.
                LaunchedEffect(deepLink) {
                    when (val d = deepLink) {
                        is DeepLinkTarget.MagicLinkExchange -> {
                            navController.navigate(MagicLinkExchangeRoute(d.token)) { launchSingleTop = true }
                            onDeepLinkConsumed()
                        }
                        is DeepLinkTarget.EmailConfirm -> {
                            navController.navigate(EmailConfirmRoute(d.key)) { launchSingleTop = true }
                            onDeepLinkConsumed()
                        }
                        is DeepLinkTarget.PasswordResetConfirm -> {
                            navController.navigate(ResetPasswordRoute(d.key)) { launchSingleTop = true }
                            onDeepLinkConsumed()
                        }
                        is DeepLinkTarget.Invitation -> {
                            navController.navigate(InvitationRoute(d.token)) { launchSingleTop = true }
                            onDeepLinkConsumed()
                        }
                        null -> Unit
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable<LoginRoute> {
                        val vm: LoginViewModel = koinInject()
                        LoginScreen(
                            viewModel = vm,
                            onLoginSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onMagicLink = {
                                navController.navigate(MagicLinkRequestRoute) { launchSingleTop = true }
                            },
                        )
                    }
                    composable<MagicLinkRequestRoute> {
                        val vm: MagicLinkRequestViewModel = koinInject()
                        MagicLinkRequestScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable<MagicLinkExchangeRoute> { entry ->
                        val args = entry.toRoute<MagicLinkExchangeRoute>()
                        val vm: MagicLinkExchangeViewModel = koinInject()
                        MagicLinkExchangeScreen(
                            viewModel = vm,
                            token = args.token,
                            onSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<EmailConfirmRoute> { entry ->
                        val args = entry.toRoute<EmailConfirmRoute>()
                        val vm: EmailConfirmViewModel = koinInject()
                        EmailConfirmScreen(
                            viewModel = vm,
                            key = args.key,
                            onSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<ResetPasswordRoute> { entry ->
                        val args = entry.toRoute<ResetPasswordRoute>()
                        val vm: ResetPasswordViewModel = koinInject()
                        ResetPasswordScreen(
                            viewModel = vm,
                            key = args.key,
                            onSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<InvitationRoute> { entry ->
                        val args = entry.toRoute<InvitationRoute>()
                        val vm: InvitationViewModel = koinInject()
                        InvitationScreen(
                            viewModel = vm,
                            token = args.token,
                            onSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<HomeRoute> {
                        val vm: DashboardViewModel = koinInject()
                        DashboardScreen(
                            viewModel = vm,
                            authRepository = authRepository,
                            onEvents = { navController.navigate(EventsListRoute) { launchSingleTop = true } },
                            onProfile = { navController.navigate(ProfileRoute) { launchSingleTop = true } },
                            onLoggedOut = {
                                navController.navigate(LoginRoute) {
                                    popUpTo<HomeRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<EventsListRoute> {
                        val vm: EventsListViewModel = koinInject()
                        EventsListScreen(
                            viewModel = vm,
                            onEventClick = { id -> navController.navigate(EventDetailRoute(id)) { launchSingleTop = true } },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<EventDetailRoute> { entry ->
                        val args = entry.toRoute<EventDetailRoute>()
                        val vm: EventDetailViewModel = koinInject()
                        EventDetailScreen(
                            viewModel = vm,
                            eventId = args.id,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<ProfileRoute> {
                        val vm: ProfileViewModel = koinInject()
                        ProfileScreen(
                            viewModel = vm,
                            languageService = languageService,
                            onChangePassword = { navController.navigate(ChangePasswordRoute) { launchSingleTop = true } },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<ChangePasswordRoute> {
                        val vm: ChangePasswordViewModel = koinInject()
                        ChangePasswordScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
        }
    }
}
