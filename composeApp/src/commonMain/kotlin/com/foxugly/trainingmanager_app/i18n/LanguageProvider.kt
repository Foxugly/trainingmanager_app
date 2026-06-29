package com.foxugly.trainingmanager_app.i18n

/**
 * Source of truth for the active UI language tag sent as `Accept-Language`.
 * S1a ships only this holder (default = fleet default "fr"); the full
 * LanguageService (optimistic switch + PATCH /me/ persistence + 5-locale
 * catalogs) is a later S1 plan.
 */
class LanguageProvider(initialTag: String = "fr") {
    var activeTag: String = initialTag
}
