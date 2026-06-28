package com.foxugly.trainingmanager_app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class StartupRouteTest {
    @Test
    fun noRefreshTokenGoesUnauthenticated() {
        assertEquals(
            StartupRoute.Unauthenticated,
            startupRoute(hasRefreshToken = false, refreshSucceeded = false),
        )
    }

    @Test
    fun refreshTokenAndSuccessfulRefreshGoesAuthenticated() {
        assertEquals(
            StartupRoute.Authenticated,
            startupRoute(hasRefreshToken = true, refreshSucceeded = true),
        )
    }

    @Test
    fun refreshTokenButFailedRefreshGoesUnauthenticated() {
        assertEquals(
            StartupRoute.Unauthenticated,
            startupRoute(hasRefreshToken = true, refreshSucceeded = false),
        )
    }
}
