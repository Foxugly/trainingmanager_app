package com.foxugly.trainingmanager_app.api.generated

import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.api.generated.models.Me
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Feasibility proof for the OpenAPI models-only codegen pipeline (feat/codegen-retry).
 *
 * Constructs and deserializes a generated model (`Me`, the backend `GET /me/` payload —
 * the generated counterpart of the hand-written `UserProfile` DTO) to prove the generated
 * kotlinx.serialization models actually compile AND round-trip on this KMP stack.
 *
 * Field names below are taken verbatim from the generated `Me.kt` / `TeamQuotaStatus.kt`
 * (note: the generated quota uses `max` + `can_create`, not `limit`).
 */
class GeneratedModelSmokeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun generatedMeDeserializesFromBackendJson() {
        val body = """
            {"id":7,"email":"a@b.co","email_confirmed":true,"is_staff":false,
             "is_superuser":false,"last_login":null,"date_joined":"2026-01-01T00:00:00Z",
             "team_quota":{"used":0,"max":3,"can_create":true},
             "calendar_token":"tok","member_id":null,
             "first_name":"Ann","last_name":"Lee","language":"fr",
             "weekly_recap_opt_in":true,"digest_email":false}
        """.trimIndent()

        val me = json.decodeFromString<Me>(body)

        assertEquals(7, me.id)
        assertEquals("a@b.co", me.email)
        assertTrue(me.emailConfirmed)
        assertEquals("Ann", me.firstName)
        assertEquals(LanguageEnum.FR, me.language)
        assertEquals(3, me.teamQuota.max)
        assertTrue(me.teamQuota.canCreate)
    }

    @Test
    fun generatedMeReencodesAndRoundTrips() {
        val original = Me(
            id = 1,
            email = "x@y.z",
            emailConfirmed = false,
            isStaff = false,
            isSuperuser = false,
            lastLogin = null,
            dateJoined = "2026-06-30T00:00:00Z",
            teamQuota = com.foxugly.trainingmanager_app.api.generated.models.TeamQuotaStatus(
                used = 1,
                max = 5,
                canCreate = true,
            ),
            calendarToken = "tok",
            memberId = null,
        )
        val round = json.decodeFromString<Me>(json.encodeToString(original))
        assertEquals(original, round)
    }
}
