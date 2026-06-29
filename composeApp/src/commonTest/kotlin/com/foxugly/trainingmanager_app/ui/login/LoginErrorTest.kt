package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginErrorTest {
    @Test fun invalidCredentialsOn401() =
        assertEquals(StringsFr.invalidCredentials, mapLoginError(ApiException(401, "login", "{}"), StringsFr))

    @Test fun emailNotVerifiedOn400WithCode() =
        assertEquals(StringsFr.emailNotVerified, mapLoginError(ApiException(400, "login", """{"code":"email_not_verified"}"""), StringsFr))

    @Test fun genericOn400WithoutThatCode() =
        assertEquals(StringsFr.loginFailed, mapLoginError(ApiException(400, "login", """{"detail":"x"}"""), StringsFr))

    @Test fun serverErrorOn500() =
        assertEquals(StringsFr.serverError, mapLoginError(ApiException(503, "login", "{}"), StringsFr))

    @Test fun offlineOnNetworkOffline() =
        assertEquals(StringsFr.networkOffline, mapLoginError(NetworkException(NetworkErrorKind.OFFLINE, "x", RuntimeException()), StringsFr))

    @Test fun timeoutOnNetworkTimeout() =
        assertEquals(StringsFr.networkTimeout, mapLoginError(NetworkException(NetworkErrorKind.TIMEOUT, "x", RuntimeException()), StringsFr))

    @Test fun nullOnCancellation() =
        assertNull(mapLoginError(CancellationException("cancelled"), StringsFr))
}
