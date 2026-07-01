package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode
import com.foxugly.trainingmanager_app.i18n.LocalAppLanguage
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.WithAppLocale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@Composable
fun EventEditorScreen(
    viewModel: EventEditorViewModel,
    eventId: Int?,
    teamId: Int?,
    onSaved: (Int) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(eventId, teamId) { viewModel.load(eventId, teamId) }

    val title = if (viewModel.isNew) s.eventEditorNewTitle else s.eventEditorEditTitle
    DetailScaffold(title = title, onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.loadError != null ->
                ErrorState(
                    viewModel.loadError!!,
                    modifier = Modifier.padding(padding),
                    onRetry = { scope.launch { viewModel.load(eventId, teamId) } },
                    retryLabel = s.retry,
                )
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                viewModel.saveError?.let {
                    ErrorBanner(it)
                    Spacer(Modifier.height(12.dp))
                }

                Field(s.eventFieldName, viewModel.name) { viewModel.name = it }

                // Team is chosen only when creating without a preset team.
                if (viewModel.isNew && viewModel.teams.isNotEmpty()) {
                    PickerField(
                        label = s.eventFieldTeam,
                        selectedText = viewModel.teams.firstOrNull { it.id == viewModel.selectedTeamId }?.name ?: "—",
                        options = viewModel.teams.map { it.id to it.name },
                        onSelect = { scope.launch { viewModel.selectTeam(it) } },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (viewModel.isNew) {
                    PickerField(
                        label = s.eventFieldProgram,
                        selectedText = viewModel.programs.firstOrNull { it.id == viewModel.selectedProgramId }?.name ?: "—",
                        options = viewModel.programs.map { it.id to it.name },
                        enabled = viewModel.programs.isNotEmpty(),
                        onSelect = { viewModel.selectProgram(it) },
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    OutlinedTextField(
                        value = viewModel.existingProgramName,
                        onValueChange = {},
                        label = { Text(s.eventFieldProgram) },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                DateField(s.eventFieldDate, viewModel.date) { viewModel.date = it }
                TimeField(s.eventFieldHourStart, viewModel.hourStart) { viewModel.hourStart = it }
                TimeField(s.eventFieldHourEnd, viewModel.hourEnd) { viewModel.hourEnd = it }
                Field(s.eventFieldLocation, viewModel.location) { viewModel.location = it }
                Field(s.eventFieldDistance, viewModel.distance, keyboard = KeyboardType.Number) { viewModel.distance = it }
                Field(s.eventGoal, viewModel.goal, singleLine = false, minLines = 2) { viewModel.goal = it }
                Field(s.eventEquipment, viewModel.equipment) { viewModel.equipment = it }
                Field(s.eventFieldDebrief, viewModel.debrief, singleLine = false, minLines = 2) { viewModel.debrief = it }

                Text(s.eventVisSection, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                VisibilityPicker(s.visShowDistance, viewModel.visDistance) { viewModel.visDistance = it }
                VisibilityPicker(s.visShowGoal, viewModel.visGoal) { viewModel.visGoal = it }
                VisibilityPicker(s.visShowRounds, viewModel.visRounds) { viewModel.visRounds = it }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { scope.launch { viewModel.save(onSaved) } },
                    enabled = viewModel.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(s.save)
                    }
                }

                if (!viewModel.isNew) {
                    Spacer(Modifier.height(8.dp))
                    var confirm by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { confirm = true },
                        enabled = !viewModel.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.eventDelete, color = MaterialTheme.colorScheme.error)
                    }
                    if (confirm) {
                        AlertDialog(
                            onDismissRequest = { confirm = false },
                            title = { Text(s.eventDelete) },
                            text = { Text(s.eventDeleteConfirm) },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirm = false
                                    scope.launch { viewModel.delete(onDeleted) }
                                }) { Text(s.eventDelete, color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = { TextButton(onClick = { confirm = false }) { Text(s.cancel) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun VisibilityPicker(label: String, value: VisibilityMode, onSelect: (VisibilityMode) -> Unit) {
    val s = LocalStrings.current
    val options = listOf(
        VisibilityMode.ALWAYS to s.visAlways,
        VisibilityMode.AFTER to s.visAfter,
        VisibilityMode.NEVER to s.visNever,
    )
    Text(label, style = MaterialTheme.typography.labelLarge)
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(options.firstOrNull { it.first == value }?.second ?: "")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (mode, text) ->
            DropdownMenuItem(text = { Text(text) }, onClick = {
                expanded = false
                onSelect(mode)
            })
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PickerField(
    label: String,
    selectedText: String,
    options: List<Pair<Int, String>>,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(selectedText)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (id, name) ->
            DropdownMenuItem(text = { Text(name) }, onClick = {
                expanded = false
                onSelect(id)
            })
        }
    }
}

/** A read-only field that opens a Material date picker; emits an ISO yyyy-MM-dd string. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun DateField(label: String, value: String, onPick: (String) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalAppLanguage.current
    Text(label, style = MaterialTheme.typography.labelLarge)
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.DateRange, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(value.ifBlank { "—" }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    if (open) {
        // Pickers follow the in-app language, not the device locale.
        WithAppLocale(lang) {
            val state = rememberDatePickerState(initialSelectedDateMillis = value.toEpochMillisOrNull())
            DatePickerDialog(
                onDismissRequest = { open = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString())
                        }
                        open = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { open = false }) { Text(s.cancel) } },
            ) {
                DatePicker(state = state)
            }
        }
    }
}

/** A read-only field that opens a Material 24h time picker; emits an HH:MM string. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, onPick: (String) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalAppLanguage.current
    Text(label, style = MaterialTheme.typography.labelLarge)
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Schedule, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(value.ifBlank { "—" }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    if (open) {
        WithAppLocale(lang) {
            val (h, m) = parseHhMm(value)
            val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
            AlertDialog(
                onDismissRequest = { open = false },
                confirmButton = {
                    TextButton(onClick = {
                        onPick("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                        open = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { open = false }) { Text(s.cancel) } },
                text = { TimePicker(state = state) },
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun String.toEpochMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}.getOrNull()

private fun parseHhMm(v: String): Pair<Int, Int> {
    val parts = v.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}
