package com.foxugly.trainingmanager_app.ui.events

import com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode
import kotlinx.datetime.LocalDate

/** Honor a VisibilityMode: always → shown, after → only once the event is past, never/null → hidden. */
fun fieldVisible(mode: VisibilityMode?, isPast: Boolean): Boolean = when (mode) {
    VisibilityMode.ALWAYS -> true
    VisibilityMode.AFTER -> isPast
    else -> false
}

/** True if the event's date (yyyy-MM-dd) is strictly before [today]. Null/unparseable → false. */
fun isEventPast(date: String?, today: LocalDate): Boolean =
    date?.let { runCatching { LocalDate.parse(it) < today }.getOrDefault(false) } ?: false
