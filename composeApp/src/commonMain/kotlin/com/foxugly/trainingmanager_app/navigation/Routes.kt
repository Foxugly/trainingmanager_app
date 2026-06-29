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
