package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import kotlinx.coroutines.launch

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

                Field(s.eventFieldDate, viewModel.date) { viewModel.date = it }
                Field(s.eventFieldHourStart, viewModel.hourStart) { viewModel.hourStart = it }
                Field(s.eventFieldHourEnd, viewModel.hourEnd) { viewModel.hourEnd = it }
                Field(s.eventFieldLocation, viewModel.location) { viewModel.location = it }
                Field(s.eventFieldDistance, viewModel.distance, keyboard = KeyboardType.Number) { viewModel.distance = it }

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
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
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
