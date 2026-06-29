package com.foxugly.trainingmanager_app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParseTest {
    @Test fun parsesHttpsMagicLink() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("ABC123"),
            parseDeepLink("https://tm.foxugly.com/auth/magic-link/ABC123"),
        )

    @Test fun parsesHttpsMagicLinkWithReturnUrlQuery() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("ABC123"),
            parseDeepLink("https://tm.foxugly.com/auth/magic-link/ABC123?returnUrl=%2Fdashboard"),
        )

    @Test fun parsesCustomSchemeMagicLink() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("TOK"),
            parseDeepLink("trainingmanager://app/auth/magic-link/TOK"),
        )

    @Test fun nullForUnknownPath() =
        assertNull(parseDeepLink("https://tm.foxugly.com/dashboard"))

    @Test fun nullForBlankToken() =
        assertNull(parseDeepLink("https://tm.foxugly.com/auth/magic-link/"))

    @Test fun nullForNullInput() = assertNull(parseDeepLink(null))

    @Test fun parsesEmailConfirm() =
        assertEquals(DeepLinkTarget.EmailConfirm("K1"), parseDeepLink("https://tm.foxugly.com/auth/confirm-email/K1"))

    @Test fun parsesResetPassword() =
        assertEquals(DeepLinkTarget.PasswordResetConfirm("uid-tok"), parseDeepLink("trainingmanager://app/auth/reset-password/uid-tok"))

    @Test fun parsesInvitation() =
        assertEquals(DeepLinkTarget.Invitation("TKN"), parseDeepLink("https://tm.foxugly.com/invitation/TKN"))
}
