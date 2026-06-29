package com.foxugly.trainingmanager_app.ui.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationTargetTest {
    @Test fun parsesEvent() =
        assertEquals(NotificationTarget.Event(5), parseNotificationTarget("/events/5"))

    @Test fun parsesTeam() =
        assertEquals(NotificationTarget.Team(3), parseNotificationTarget("/teams/3"))

    @Test fun teamFromNestedTopicUrl() =
        assertEquals(NotificationTarget.Team(3), parseNotificationTarget("/teams/3/topics/7"))

    @Test fun nullForUnknown() = assertNull(parseNotificationTarget("/dashboard"))

    @Test fun nullForNull() = assertNull(parseNotificationTarget(null))
}
