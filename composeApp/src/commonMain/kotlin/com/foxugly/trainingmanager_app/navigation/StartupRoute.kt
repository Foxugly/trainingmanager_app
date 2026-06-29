package com.foxugly.trainingmanager_app.navigation

/** Where the app lands after the startup auth check. The full route graph
 * (Navigation Compose) is a later S1 plan; S1a only needs this three-way stub. */
enum class StartupRoute { Loading, Authenticated, Unauthenticated }

/**
 * Pure bootstrap decision (S1 spec §6): no refresh token → Unauthenticated;
 * refresh token + successful refresh → Authenticated; otherwise (refresh failed)
 * → Unauthenticated. Kept side-effect-free so it's unit-testable on the JVM host.
 */
fun startupRoute(hasRefreshToken: Boolean, refreshSucceeded: Boolean): StartupRoute =
    if (hasRefreshToken && refreshSucceeded) StartupRoute.Authenticated
    else StartupRoute.Unauthenticated
