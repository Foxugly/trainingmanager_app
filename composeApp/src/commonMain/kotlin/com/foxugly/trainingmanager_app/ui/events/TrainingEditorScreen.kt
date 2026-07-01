package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.Exercise
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

                if (viewModel.hasTraining) {
                    viewModel.rounds.forEach { round ->
                        val title = s.roundLabel(round.order) + if (round.count > 1) " ×${round.count}" else ""
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        round.exercises.sortedBy { it.order ?: 0 }.forEach { ex -> ExerciseLine(ex) }
                        Spacer(Modifier.height(12.dp))
                    }

                    var confirm by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { confirm = true },
                        enabled = !viewModel.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (viewModel.isClearing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(s.trainingClear, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (confirm) {
                        AlertDialog(
                            onDismissRequest = { confirm = false },
                            title = { Text(s.trainingClear) },
                            text = { Text(s.trainingClearConfirm) },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirm = false
                                    scope.launch { viewModel.clear() }
                                }) { Text(s.trainingClear, color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = { TextButton(onClick = { confirm = false }) { Text(s.cancel) } },
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
                    Button(
                        onClick = { scope.launch { viewModel.generate() } },
                        enabled = !viewModel.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
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
}

@Composable
private fun ExerciseLine(ex: Exercise) {
    val notes = ex.notes.orEmpty()
    val main = buildString {
        if ((ex.repetition ?: 0) > 0) append("${ex.repetition} × ")
        if ((ex.distance ?: 0) > 0) append("${ex.distance} m")
    }.trim().ifBlank { notes.ifBlank { "—" } }
    val meta = listOfNotNull(
        ex.modality.name.ifBlank { null },
        ex.energysegment.abv.ifBlank { null },
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp)) {
        Text(main, style = MaterialTheme.typography.bodyMedium)
        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
    }
}
