package com.foxugly.trainingmanager_app.data.storage

import com.foxugly.trainingmanager_app.FakeTokenStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeTokenStoreTest {

    @Test
    fun storesAndReadsBackTokensAndRemember() {
        val store = FakeTokenStore()
        assertNull(store.getAccessToken())
        assertFalse(store.getRemember())

        store.setAccessToken("a")
        store.setRefreshToken("r")
        store.setRemember(true)

        assertEquals("a", store.getAccessToken())
        assertEquals("r", store.getRefreshToken())
        assertTrue(store.getRemember())
    }

    @Test
    fun clearAuthTokensWipesAccessAndRefreshAndFlagsCleared() {
        val store = FakeTokenStore(access = "a", refresh = "r")
        store.clearAuthTokens()
        assertNull(store.getAccessToken())
        assertNull(store.getRefreshToken())
        assertTrue(store.cleared)
    }

    /**
     * Verifies the platform actuals' contract: [TokenStore.clearAuthTokens] clears
     * the JWT pair but must NOT touch the `remember` preference — the user's choice
     * to stay logged-in persists across token refreshes and explicit logouts so that
     * a re-login can honour it without prompting again.
     */
    @Test
    fun clearAuthTokensDoesNotWipeRemember() {
        val store = FakeTokenStore(access = "a", refresh = "r", remember = true)
        store.clearAuthTokens()
        assertNull(store.getAccessToken())
        assertNull(store.getRefreshToken())
        assertTrue(store.getRemember(), "remember must stay true after clearAuthTokens()")
    }
}
