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
