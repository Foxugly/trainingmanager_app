package com.foxugly.trainingmanager_app.ui.notifications

/** A tappable destination derived from a notification's `url` (a frontend path). */
sealed interface NotificationTarget {
    data class Event(val id: Int) : NotificationTarget
    data class Team(val id: Int) : NotificationTarget
}

// Whitelist: only an app-relative path starting with one of these segments is
// routed. Anything else (absolute URL, other host, other screen) is ignored.
private val EVENT_PATH = Regex("""^/events/(\d{1,9})(?:[/?#].*)?$""")
private val TEAM_PATH = Regex("""^/teams/(\d{1,9})(?:[/?#].*)?$""")

/** Map a notification url like "/events/5" or "/teams/3" (or "/teams/3/topics/7") to a target. */
fun parseNotificationTarget(url: String?): NotificationTarget? {
    if (url == null) return null
    EVENT_PATH.matchEntire(url)?.let { return NotificationTarget.Event(it.groupValues[1].toInt()) }
    TEAM_PATH.matchEntire(url)?.let { return NotificationTarget.Team(it.groupValues[1].toInt()) }
    return null
}
