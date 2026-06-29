package com.foxugly.trainingmanager_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fleet brand theme — emerald accent over slate neutrals, mirroring the
 * TrainingManager web charte so the native app reads as the same family. Ships
 * with a dark scheme scaffold (parity to be refined alongside the screens).
 */

// Emerald accent (web --accent / --accent-strong). The strong shade is the
// button fill — white-on-#059669 clears the contrast bar that #10b981 fails.
private val EmeraldStrong = Color(0xFF059669)
private val Emerald = Color(0xFF10B981)
private val EmeraldContainer = Color(0xFFD1FAE5) // emerald-100
private val OnEmeraldContainer = Color(0xFF065F46) // emerald-800

// Slate neutrals.
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Error = Color(0xFFDC2626)

val TmLightColors: ColorScheme = lightColorScheme(
    primary = EmeraldStrong,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = Emerald,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

val TmDarkColors: ColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Slate900,
    primaryContainer = OnEmeraldContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = Emerald,
    onSecondary = Slate900,
    secondaryContainer = OnEmeraldContainer,
    onSecondaryContainer = EmeraldContainer,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate500,
    outlineVariant = Slate700,
    error = Color(0xFFF87171),
    onError = Slate900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

/** Applies the fleet brand palette, following the system dark-mode setting. */
@Composable
fun TrainingManagerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) TmDarkColors else TmLightColors,
        content = content,
    )
}
