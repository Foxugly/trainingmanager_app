package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.foxugly.trainingmanager_app.api.generated.models.CustomUserPublic
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    teamId: Int,
    onDiscussions: () -> Unit,
    onCreateEvent: () -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    DetailScaffold(
        title = viewModel.team?.name ?: s.teamsTitle,
        onBack = onBack,
        actions = { TextButton(onClick = onDiscussions) { Text(s.discussionsEntry) } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null || viewModel.team == null ->
                    ErrorState(viewModel.error ?: s.teamLoadFailed, onRetry = { scope.launch { viewModel.load(teamId) } }, retryLabel = s.retry)
                else -> {
                    val team = viewModel.team!!
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        if (viewModel.canManage) {
                            Button(onClick = onCreateEvent, modifier = Modifier.fillMaxWidth()) {
                                Text(s.addEvent)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        team.sport.name.takeIf { it.isNotBlank() }?.let { Labeled(s.teamSport, it) }
                        Labeled(s.teamOwner, fullName(team.owner))
                        if (team.managers.isNotEmpty()) {
                            Labeled(s.teamManagers, team.managers.joinToString(", ") { fullName(it) })
                        }
                        if (viewModel.members.isNotEmpty()) {
                            Labeled(
                                s.teamMembers,
                                viewModel.members.joinToString(", ") {
                                    it.fullname.ifBlank { listOf(it.firstname, it.lastname).filter { p -> p.isNotBlank() }.joinToString(" ") }
                                },
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(s.programsSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        viewModel.programError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                        if (viewModel.programs.isEmpty()) {
                            Text(s.programsEmpty, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            viewModel.programs.forEach { p ->
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        if (viewModel.canManage) {
                            var showAddProgram by remember { mutableStateOf(false) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showAddProgram = true },
                                enabled = !viewModel.isSavingProgram,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(s.addProgram) }
                            if (showAddProgram) {
                                AddNameDialog(
                                    title = s.addProgram,
                                    label = s.programName,
                                    onDismiss = { showAddProgram = false },
                                    onConfirm = { name -> showAddProgram = false; scope.launch { viewModel.addProgram(name) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fullName(u: CustomUserPublic): String =
    listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "#${u.id}" }

@Composable
private fun Labeled(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Simple dialog that asks for a single name and confirms it. */
@Composable
fun AddNameDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
