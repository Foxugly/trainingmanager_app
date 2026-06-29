package com.foxugly.trainingmanager_app.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object MagicLinkRequestRoute

@Serializable
data class MagicLinkExchangeRoute(val token: String)

@Serializable
data class EmailConfirmRoute(val key: String)

@Serializable
data class ResetPasswordRoute(val key: String)

@Serializable
data class InvitationRoute(val token: String)

@Serializable
object ProfileRoute

@Serializable
object ChangePasswordRoute

@Serializable
object EventsListRoute

@Serializable
data class EventDetailRoute(val id: Int)

@Serializable
object TeamsListRoute

@Serializable
data class TeamDetailRoute(val id: Int)

@Serializable
data class TopicsListRoute(val teamId: Int)

@Serializable
data class TopicThreadRoute(val teamId: Int, val topicId: Int, val allowReplies: Boolean)

@Serializable
object NotificationsRoute

@Serializable
object RegisterRoute

@Serializable
object ForgotPasswordRoute
