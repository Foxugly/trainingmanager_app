package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.AttendanceStatus
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

/**
 * Inline attendance management (roster + per-member status picker), rendered
 * directly inside the event detail's "Présences" tab. Gated to managers by the
 * caller. Loads the roster/statuses on first composition for [eventId].
 */
@Composable
fun AttendanceContent(viewModel: AttendanceViewModel, eventId: Int) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    when {
        viewModel.isLoading ->
            Text(s.attendanceTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        viewModel.loadError != null -> ErrorBanner(viewModel.loadError!!)
        viewModel.rows.isEmpty() ->
            Text(s.attendanceEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Column(Modifier.fillMaxWidth()) {
            viewModel.actionError?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(12.dp))
            }
            viewModel.rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(row.memberName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatusPicker(
                        statuses = viewModel.statuses,
                        selectedId = row.statusId,
                        enabled = !viewModel.isSaving,
                        onSelect = { scope.launch { viewModel.setStatus(row.memberId, it) } },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatusPicker(
    statuses: List<AttendanceStatus>,
    selectedId: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = statuses.firstOrNull { it.id == selectedId }?.label ?: "—"
    OutlinedButton(onClick = { expanded = true }, enabled = enabled && statuses.isNotEmpty()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        statuses.forEach { status ->
            DropdownMenuItem(text = { Text(status.label) }, onClick = {
                expanded = false
                onSelect(status.id)
            })
        }
    }
}
