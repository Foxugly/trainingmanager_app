package com.foxugly.trainingmanager_app.ui.notifications

/** A tappable destination derived from a notification's `url` (a frontend path). */
sealed interface NotificationTarget {
    data class Event(val id: Int) : NotificationTarget
    data class Team(val id: Int) : NotificationTarget
}

/** Map a notification url like "/events/5" or "/teams/3" (or "/teams/3/topics/7") to a target. */
fun parseNotificationTarget(url: String?): NotificationTarget? {
    if (url == null) return null
    Regex("/events/(\\d+)").find(url)?.let { return NotificationTarget.Event(it.groupValues[1].toInt()) }
    Regex("/teams/(\\d+)").find(url)?.let { return NotificationTarget.Team(it.groupValues[1].toInt()) }
    return null
}
