package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun tokenObtainRequestSerializesRememberAndOmitsNothingExtra() {
        val body = json.encodeToString(
            TokenObtainRequest(email = "a@b.co", password = "pw", remember = true),
        )
        assertTrue(body.contains("\"email\":\"a@b.co\""))
        assertTrue(body.contains("\"remember\":true"))
    }

    @Test
    fun refreshResponseDecodesWithAndWithoutRotatedRefresh() {
        val rotated = json.decodeFromString<RefreshResponse>("""{"access":"x","refresh":"y"}""")
        assertEquals("x", rotated.access)
        assertEquals("y", rotated.refresh)

        val noRotation = json.decodeFromString<RefreshResponse>("""{"access":"x"}""")
        assertNull(noRotation.refresh)
    }

    @Test
    fun userProfileToleratesMissingOptionalFields() {
        val me = json.decodeFromString<UserProfile>(
            """{"id":7,"email":"a@b.co","email_confirmed":true,"language":"fr"}""",
        )
        assertEquals(7, me.id)
        assertEquals("fr", me.language)
        assertNull(me.firstName)
    }
}
