package com.foxugly.trainingmanager_app.ui.components

/**
 * Strip the server's sanitized HTML to plain text for display (no HTML renderer
 * needed). Shared by discussion messages and event rich-text fields (goal,
 * equipment). Plain text passes through unchanged.
 */
internal fun stripHtml(s: String): String =
    s.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'")
        .trim()
