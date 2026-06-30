package com.foxugly.trainingmanager_app.ui.events

import com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventVisibilityTest {
    @Test fun always() = assertTrue(fieldVisible(VisibilityMode.ALWAYS, false))
    @Test fun never() = assertFalse(fieldVisible(VisibilityMode.NEVER, true))
    @Test fun nullHidden() = assertFalse(fieldVisible(null, true))
    @Test fun afterHiddenBeforePast() = assertFalse(fieldVisible(VisibilityMode.AFTER, false))
    @Test fun afterShownWhenPast() = assertTrue(fieldVisible(VisibilityMode.AFTER, true))

    private val today = LocalDate(2026, 6, 29)
    @Test fun pastDateIsPast() = assertTrue(isEventPast("2020-01-01", today))
    @Test fun futureDateIsNotPast() = assertFalse(isEventPast("2030-01-01", today))
    @Test fun nullDateNotPast() = assertEquals(false, isEventPast(null, today))
}
