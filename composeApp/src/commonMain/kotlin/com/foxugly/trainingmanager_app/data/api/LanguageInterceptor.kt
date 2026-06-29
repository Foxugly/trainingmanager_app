package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.i18n.LanguageProvider
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/** Attaches `Accept-Language: <activeTag>` to every request so the backend
 * localizes translatable model fields + error labels. */
class LanguageInterceptor(private val languageProvider: LanguageProvider) {
    val plugin = createClientPlugin("LanguageInterceptor") {
        onRequest { request, _ ->
            request.headers[HttpHeaders.AcceptLanguage] = languageProvider.activeTag
        }
    }
}
