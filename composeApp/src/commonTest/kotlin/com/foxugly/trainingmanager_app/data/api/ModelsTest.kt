package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.api.generated.models.TokenRefresh
import com.foxugly.trainingmanager_app.api.generated.models.VerifiedTokenObtainPairRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hand-written auth DTOs (TokenObtainRequest / TokenPair / RefreshRequest / RefreshResponse)
 * were replaced by the OpenAPI-generated VerifiedTokenObtainPairRequest / TokenObtainPairResponse /
 * TokenRefreshRequest / TokenRefresh. These two checks lock in the two properties that matter for
 * auth correctness on the generated models: the login request still carries `remember`, and the
 * refresh response decodes the rotated refresh token.
 */
class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun verifiedTokenObtainPairRequestSerializesRemember() {
        // `remember` (7d-vs-30d refresh TTL) MUST reach the backend.
        val body = json.encodeToString(
            VerifiedTokenObtainPairRequest(email = "a@b.co", password = "pw", remember = true),
        )
        assertTrue(body.contains("\"email\":\"a@b.co\""))
        assertTrue(body.contains("\"remember\":true"))
    }

    @Test
    fun tokenRefreshDecodesRotatedRefresh() {
        // The backend rotates + blacklists refresh tokens, so each refresh returns a NEW refresh.
        // The generated model marks `refresh` @Required (rotation is always on) — it must decode here.
        val rotated = json.decodeFromString<TokenRefresh>("""{"access":"x","refresh":"y"}""")
        assertEquals("x", rotated.access)
        assertEquals("y", rotated.refresh)
    }
}
