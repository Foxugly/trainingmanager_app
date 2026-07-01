package com.foxugly.trainingmanager_app.ui.teams

import com.foxugly.trainingmanager_app.api.generated.models.Team

/**
 * True if [meId] is the team owner or one of its managers — i.e. may create /
 * edit the team's events and trainings. The server enforces the same rule
 * (403 "Not a manager of this event team"); this only gates the UI affordances.
 */
fun Team.isManagedBy(meId: Int?): Boolean =
    meId != null && (owner.id == meId || managers.any { it.id == meId })
