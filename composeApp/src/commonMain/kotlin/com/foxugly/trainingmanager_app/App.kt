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
import com.foxugly.trainingmanager_app.navigation.EventEditorRoute
import com.foxugly.trainingmanager_app.navigation.EventsListRoute
import com.foxugly.trainingmanager_app.navigation.HomeRoute
import com.foxugly.trainingmanager_app.navigation.InvitationRoute
import com.foxugly.trainingmanager_app.navigation.ChangePasswordRoute
import com.foxugly.trainingmanager_app.navigation.LoginRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkExchangeRoute
import com.foxugly.trainingmanager_app.navigation.ForgotPasswordRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkRequestRoute
import com.foxugly.trainingmanager_app.navigation.NotificationsRoute
import com.foxugly.trainingmanager_app.navigation.RegisterRoute
import com.foxugly.trainingmanager_app.navigation.ProfileRoute
import com.foxugly.trainingmanager_app.navigation.ResetPasswordRoute
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.TeamDetailRoute
import com.foxugly.trainingmanager_app.navigation.TeamsListRoute
import com.foxugly.trainingmanager_app.navigation.TopicThreadRoute
import com.foxugly.trainingmanager_app.navigation.TopicsListRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmScreen
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmViewModel
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordScreen
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordViewModel
import com.foxugly.trainingmanager_app.ui.dashboard.DashboardScreen
import com.foxugly.trainingmanager_app.ui.dashboard.DashboardViewModel
import com.foxugly.trainingmanager_app.ui.events.EventDetailScreen
import com.foxugly.trainingmanager_app.ui.events.EventDetailViewModel
import com.foxugly.trainingmanager_app.ui.events.EventEditorScreen
import com.foxugly.trainingmanager_app.ui.events.EventEditorViewModel
import com.foxugly.trainingmanager_app.ui.events.EventsListScreen
import com.foxugly.trainingmanager_app.ui.events.EventsListViewModel
import com.foxugly.trainingmanager_app.ui.invitation.InvitationScreen
import com.foxugly.trainingmanager_app.ui.invitation.InvitationViewModel
import com.foxugly.trainingmanager_app.platform.FcmTokenProvider
import com.foxugly.trainingmanager_app.ui.forgot.ForgotPasswordScreen
import com.foxugly.trainingmanager_app.ui.forgot.ForgotPasswordViewModel
import com.foxugly.trainingmanager_app.ui.login.LoginScreen
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.register.RegisterScreen
import com.foxugly.trainingmanager_app.ui.register.RegisterViewModel
import com.foxugly.trainingmanager_app.ui.notifications.NotificationTarget
import com.foxugly.trainingmanager_app.ui.notifications.parseNotificationTarget
import com.foxugly.trainingmanager_app.ui.notifications.NotificationsScreen
import com.foxugly.trainingmanager_app.ui.notifications.NotificationsViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
import com.foxugly.trainingmanager_app.ui.profile.ChangePasswordScreen
import com.foxugly.trainingmanager_app.ui.profile.ChangePasswordViewModel
import com.foxugly.trainingmanager_app.ui.profile.ProfileScreen
import com.foxugly.trainingmanager_app.ui.profile.ProfileViewModel
import com.foxugly.trainingmanager_app.ui.teams.TeamDetailScreen
import com.foxugly.trainingmanager_app.ui.teams.TeamDetailViewModel
import com.foxugly.trainingmanager_app.ui.teams.TeamsListScreen
import com.foxugly.trainingmanager_app.ui.teams.TeamsListViewModel
import com.foxugly.trainingmanager_app.ui.discussions.TopicThreadScreen
import com.foxugly.trainingmanager_app.ui.discussions.TopicThreadViewModel
import com.foxugly.trainingmanager_app.ui.discussions.TopicsListScreen
import com.foxugly.trainingmanager_app.ui.discussions.TopicsListViewModel
import com.foxugly.trainingmanager_app.ui.components.MainTab
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Bottom-nav switch between the five top-level destinations: single back-stack
 * entry per tab, state saved/restored, so switching never piles up history. */
private fun NavController.selectTab(tab: MainTab) {
    val route: Any = when (tab) {
        MainTab.DASHBOARD -> HomeRoute
        MainTab.EVENTS -> EventsListRoute
        MainTab.TEAMS -> TeamsListRoute
        MainTab.NOTIFICATIONS -> NotificationsRoute
        MainTab.PROFILE -> ProfileRoute
    }
    navigate(route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun App(
    authRepository: AuthRepository = koinInject(),
    languageService: LanguageService = koinInject(),
    fcmTokenProvider: FcmTokenProvider = koinInject(),
    deepLink: DeepLinkTarget? = null,
    onDeepLinkConsumed: () -> Unit = {},
    notificationUrl: String? = null,
    onNotificationConsumed: () -> Unit = {},
) {
    var route by remember { mutableStateOf(StartupRoute.Loading) }
    LaunchedEffect(authRepository) {
        val hasRefresh = authRepository.hasRefreshToken()
        val refreshed = hasRefresh && authRepository.tryRefresh()
        val resolved = startupRoute(hasRefresh, refreshed)
        // Initialize the UI language from the signed-in user's preference BEFORE first render
        // so the initial frame is localized.
        if (resolved == StartupRoute.Authenticated) {
            authRepository.getCurrentUser().getOrNull()?.language?.value?.let { languageService.setActive(it) }
        }
        route = resolved
        // FCM token fetch + device registration are pure side effects; run them off the
        // critical path so they never gate first render.
        if (resolved == StartupRoute.Authenticated) {
            launch {
                // Register this device's FCM token for push (best-effort; null on iOS until the SDK is wired).
                fcmTokenProvider.token()?.let { token ->
                    authRepository.registerDevice(token, fcmTokenProvider.platform)
                }
            }
        }
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

                // Route a tapped push notification to its target (signed-in only).
                LaunchedEffect(notificationUrl, route) {
                    if (route != StartupRoute.Authenticated) return@LaunchedEffect
                    when (val t = parseNotificationTarget(notificationUrl)) {
                        is NotificationTarget.Event -> {
                            navController.navigate(EventDetailRoute(t.id)) { launchSingleTop = true }
                            onNotificationConsumed()
                        }
                        is NotificationTarget.Team -> {
                            navController.navigate(TeamDetailRoute(t.id)) { launchSingleTop = true }
                            onNotificationConsumed()
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
                            onCreateAccount = {
                                navController.navigate(RegisterRoute) { launchSingleTop = true }
                            },
                            onForgotPassword = {
                                navController.navigate(ForgotPasswordRoute) { launchSingleTop = true }
                            },
                        )
                    }
                    composable<MagicLinkRequestRoute> {
                        val vm: MagicLinkRequestViewModel = koinInject()
                        MagicLinkRequestScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable<RegisterRoute> {
                        val vm: RegisterViewModel = koinInject()
                        RegisterScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable<ForgotPasswordRoute> {
                        val vm: ForgotPasswordViewModel = koinInject()
                        ForgotPasswordScreen(viewModel = vm, onBack = { navController.popBackStack() })
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
                            onSelectTab = navController::selectTab,
                            onTeamClick = { id -> navController.navigate(TeamDetailRoute(id)) { launchSingleTop = true } },
                        )
                    }
                    composable<EventsListRoute> {
                        val vm: EventsListViewModel = koinInject()
                        EventsListScreen(
                            viewModel = vm,
                            onSelectTab = navController::selectTab,
                            onEventClick = { id -> navController.navigate(EventDetailRoute(id)) { launchSingleTop = true } },
                            onCreateEvent = { navController.navigate(EventEditorRoute()) { launchSingleTop = true } },
                        )
                    }
                    composable<EventDetailRoute> { entry ->
                        val args = entry.toRoute<EventDetailRoute>()
                        val vm: EventDetailViewModel = koinInject()
                        EventDetailScreen(
                            viewModel = vm,
                            eventId = args.id,
                            onEdit = { navController.navigate(EventEditorRoute(eventId = args.id)) { launchSingleTop = true } },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<EventEditorRoute> { entry ->
                        val args = entry.toRoute<EventEditorRoute>()
                        val vm: EventEditorViewModel = koinInject()
                        EventEditorScreen(
                            viewModel = vm,
                            eventId = args.eventId,
                            teamId = args.teamId,
                            onSaved = { id ->
                                navController.navigate(EventDetailRoute(id)) {
                                    popUpTo<EventEditorRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onDeleted = { navController.popBackStack() },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<TeamsListRoute> {
                        val vm: TeamsListViewModel = koinInject()
                        TeamsListScreen(
                            viewModel = vm,
                            onSelectTab = navController::selectTab,
                            onTeamClick = { id -> navController.navigate(TeamDetailRoute(id)) { launchSingleTop = true } },
                        )
                    }
                    composable<TeamDetailRoute> { entry ->
                        val args = entry.toRoute<TeamDetailRoute>()
                        val vm: TeamDetailViewModel = koinInject()
                        TeamDetailScreen(
                            viewModel = vm,
                            teamId = args.id,
                            onDiscussions = { navController.navigate(TopicsListRoute(args.id)) { launchSingleTop = true } },
                            onCreateEvent = { navController.navigate(EventEditorRoute(teamId = args.id)) { launchSingleTop = true } },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<TopicsListRoute> { entry ->
                        val args = entry.toRoute<TopicsListRoute>()
                        val vm: TopicsListViewModel = koinInject()
                        TopicsListScreen(
                            viewModel = vm,
                            teamId = args.teamId,
                            onTopicClick = { topic ->
                                navController.navigate(TopicThreadRoute(args.teamId, topic.id, topic.allowAthleteReplies ?: false)) { launchSingleTop = true }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<TopicThreadRoute> { entry ->
                        val args = entry.toRoute<TopicThreadRoute>()
                        val vm: TopicThreadViewModel = koinInject()
                        TopicThreadScreen(
                            viewModel = vm,
                            teamId = args.teamId,
                            topicId = args.topicId,
                            allowReplies = args.allowReplies,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<NotificationsRoute> {
                        val vm: NotificationsViewModel = koinInject()
                        NotificationsScreen(
                            viewModel = vm,
                            onSelectTab = navController::selectTab,
                            onOpen = { target ->
                                when (target) {
                                    is NotificationTarget.Event -> navController.navigate(EventDetailRoute(target.id)) { launchSingleTop = true }
                                    is NotificationTarget.Team -> navController.navigate(TeamDetailRoute(target.id)) { launchSingleTop = true }
                                }
                            },
                        )
                    }
                    composable<ProfileRoute> {
                        val vm: ProfileViewModel = koinInject()
                        ProfileScreen(
                            viewModel = vm,
                            languageService = languageService,
                            authRepository = authRepository,
                            onSelectTab = navController::selectTab,
                            onChangePassword = { navController.navigate(ChangePasswordRoute) { launchSingleTop = true } },
                            onLoggedOut = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
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
