package com.foxugly.trainingmanager_app.ui.events

import kotlinx.datetime.LocalDate

/** Honor a VisibilityMode: always → shown, after → only once the event is past, never → hidden. */
fun fieldVisible(mode: String, isPast: Boolean): Boolean = when (mode) {
    "always" -> true
    "after" -> isPast
    else -> false
}

/** True if the event's date (yyyy-MM-dd) is strictly before [today]. Null/unparseable → false. */
fun isEventPast(date: String?, today: LocalDate): Boolean =
    date?.let { runCatching { LocalDate.parse(it) < today }.getOrDefault(false) } ?: false
