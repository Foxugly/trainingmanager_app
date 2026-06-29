package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginErrorTest {
    @Test fun invalidCredentialsOn401() =
        assertEquals(LoginStrings.invalidCredentials, mapLoginError(ApiException(401, "login", "{}")))

    @Test fun emailNotVerifiedOn400WithCode() =
        assertEquals(LoginStrings.emailNotVerified, mapLoginError(ApiException(400, "login", """{"code":"email_not_verified"}""")))

    @Test fun genericOn400WithoutThatCode() =
        assertEquals(LoginStrings.loginFailed, mapLoginError(ApiException(400, "login", """{"detail":"x"}""")))

    @Test fun serverErrorOn500() =
        assertEquals(LoginStrings.serverError, mapLoginError(ApiException(503, "login", "{}")))

    @Test fun offlineOnNetworkOffline() =
        assertEquals(LoginStrings.networkOffline, mapLoginError(NetworkException(NetworkErrorKind.OFFLINE, "x", RuntimeException())))

    @Test fun timeoutOnNetworkTimeout() =
        assertEquals(LoginStrings.networkTimeout, mapLoginError(NetworkException(NetworkErrorKind.TIMEOUT, "x", RuntimeException())))

    @Test fun nullOnCancellation() =
        assertNull(mapLoginError(CancellationException("cancelled")))
}
