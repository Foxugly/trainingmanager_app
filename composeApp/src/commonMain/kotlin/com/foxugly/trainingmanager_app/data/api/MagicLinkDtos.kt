package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.Serializable

/** POST auth/magic-link/request/ — always-200, no captcha. */
@Serializable
data class MagicLinkRequestBody(val email: String)

/** POST auth/magic-link/exchange/ — trades a magic token for a JWT pair. */
@Serializable
data class MagicLinkExchangeBody(val token: String)
