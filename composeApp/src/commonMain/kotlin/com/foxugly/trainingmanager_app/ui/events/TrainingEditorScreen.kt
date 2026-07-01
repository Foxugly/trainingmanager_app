package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.EnergySegment
import com.foxugly.trainingmanager_app.api.generated.models.Exercise
import com.foxugly.trainingmanager_app.api.generated.models.Modality
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun TrainingEditorScreen(
    viewModel: TrainingEditorViewModel,
    eventId: Int,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    // Exercise dialog target: the round to add/edit an exercise in (null = closed).
    var dialogRoundId by remember { mutableStateOf<Int?>(null) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }

    DetailScaffold(title = s.trainingTitle, onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.loadError != null ->
                ErrorState(
                    viewModel.loadError!!,
                    modifier = Modifier.padding(padding),
                    onRetry = { scope.launch { viewModel.load(eventId) } },
                    retryLabel = s.retry,
                )
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                viewModel.actionError?.let {
                    ErrorBanner(it)
                    Spacer(Modifier.height(12.dp))
                }
                if (viewModel.generatedOk) {
                    Text(s.trainingGeneratedOk, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }

                viewModel.rounds.forEach { round ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            s.roundLabel(round.order),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { scope.launch { viewModel.setRoundCount(round.id, round.count - 1) } }, enabled = !viewModel.busy && round.count > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = null)
                        }
                        Text("×${round.count}", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { scope.launch { viewModel.setRoundCount(round.id, round.count + 1) } }, enabled = !viewModel.busy) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                        IconButton(onClick = { scope.launch { viewModel.removeRound(round.id) } }, enabled = !viewModel.busy) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    round.exercises.sortedBy { it.order ?: 0 }.forEach { ex ->
                        ExerciseEditRow(
                            ex = ex,
                            enabled = !viewModel.busy,
                            onEdit = { editingExercise = ex; dialogRoundId = round.id },
                            onDelete = { scope.launch { viewModel.removeExercise(ex.id) } },
                        )
                    }
                    TextButton(onClick = { editingExercise = null; dialogRoundId = round.id }, enabled = !viewModel.busy) {
                        Text(s.addExercise)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedButton(onClick = { scope.launch { viewModel.addRound() } }, enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(s.addRound)
                }
                Spacer(Modifier.height(16.dp))

                if (viewModel.hasTraining) {
                    var confirmClear by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { confirmClear = true }, enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth()) {
                        if (viewModel.isClearing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(s.trainingClear, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (confirmClear) {
                        AlertDialog(
                            onDismissRequest = { confirmClear = false },
                            title = { Text(s.trainingClear) },
                            text = { Text(s.trainingClearConfirm) },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmClear = false
                                    scope.launch { viewModel.clear() }
                                }) { Text(s.trainingClear, color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(s.cancel) } },
                        )
                    }
                } else {
                    Text(s.trainingEmpty, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.additionalPrompt,
                        onValueChange = { viewModel.additionalPrompt = it },
                        label = { Text(s.trainingPromptHint) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { viewModel.generate() } }, enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth()) {
                        if (viewModel.isGenerating) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(s.trainingGenerate)
                        }
                    }
                }
            }
        }
    }

    val roundId = dialogRoundId
    if (roundId != null) {
        ExerciseDialog(
            modalities = viewModel.modalities,
            zones = viewModel.energySegments,
            initial = editingExercise,
            onDismiss = { dialogRoundId = null },
            onSave = { modalityId, zoneId, reps, distance, notes ->
                val editing = editingExercise
                dialogRoundId = null
                scope.launch {
                    if (editing == null) {
                        viewModel.addExercise(roundId, modalityId, zoneId, reps, distance, notes)
                    } else {
                        viewModel.updateExercise(editing.id, modalityId, zoneId, reps, distance, notes)
                    }
                }
            },
        )
    }
}

@Composable
private fun ExerciseEditRow(ex: Exercise, enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val main = buildString {
        if ((ex.repetition ?: 0) > 0) append("${ex.repetition} × ")
        if ((ex.distance ?: 0) > 0) append("${ex.distance} m")
    }.trim().ifBlank { ex.notes.orEmpty().ifBlank { "—" } }
    val meta = listOfNotNull(ex.modality.name.ifBlank { null }, ex.energysegment.abv.ifBlank { null }).joinToString(" · ")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).clickable(enabled = enabled, onClick = onEdit).padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
            Text(main, style = MaterialTheme.typography.bodyMedium)
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ExerciseDialog(
    modalities: List<Modality>,
    zones: List<EnergySegment>,
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (modalityId: Int, zoneId: Int, reps: Long?, distance: Long?, notes: String?) -> Unit,
) {
    val s = LocalStrings.current
    var modalityId by remember { mutableStateOf(initial?.modality?.id ?: modalities.firstOrNull()?.id) }
    var zoneId by remember { mutableStateOf(initial?.energysegment?.id ?: zones.firstOrNull()?.id) }
    var reps by remember { mutableStateOf(initial?.repetition?.takeIf { it > 0 }?.toString().orEmpty()) }
    var distance by remember { mutableStateOf(initial?.distance?.takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.exerciseDialogTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DialogPicker(s.exerciseModality, modalities.firstOrNull { it.id == modalityId }?.name ?: "—", modalities.map { it.id to it.name }) { modalityId = it }
                Spacer(Modifier.height(8.dp))
                DialogPicker(s.exerciseZone, zones.firstOrNull { it.id == zoneId }?.abv ?: "—", zones.map { it.id to it.abv }) { zoneId = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(reps, { reps = it }, label = { Text(s.exerciseReps) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(distance, { distance = it }, label = { Text(s.eventFieldDistance) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text(s.exerciseNotes) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val m = modalityId
            val z = zoneId
            TextButton(
                onClick = { if (m != null && z != null) onSave(m, z, reps.trim().toLongOrNull(), distance.trim().toLongOrNull(), notes) },
                enabled = m != null && z != null,
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun DialogPicker(label: String, selectedText: String, options: List<Pair<Int, String>>, onSelect: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, enabled = options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
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
