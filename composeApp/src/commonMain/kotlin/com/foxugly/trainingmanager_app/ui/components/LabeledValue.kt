package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A read-only field: an optional bold label above its value. Shared across the
 * team / event / program detail screens (replaces the per-screen copies of
 * `Labeled` / `Field` / `ProgramField`).
 */
@Composable
fun LabeledValue(label: String?, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
