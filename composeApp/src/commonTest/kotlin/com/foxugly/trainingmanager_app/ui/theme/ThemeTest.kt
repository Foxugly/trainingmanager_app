package com.foxugly.trainingmanager_app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThemeTest {
    @Test
    fun lightSchemeUsesEmeraldStrongPrimary() {
        assertEquals(Color(0xFF059669), TmLightColors.primary)
    }

    @Test
    fun darkSchemeDiffersFromLightBackground() {
        assertNotEquals(TmLightColors.background, TmDarkColors.background)
    }
}
