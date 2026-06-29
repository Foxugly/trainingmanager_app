package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.data.api.CustomUserPublic
import com.foxugly.trainingmanager_app.i18n.LocalStrings

@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    teamId: Int,
    onDiscussions: () -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(viewModel.team?.name ?: s.teamsTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDiscussions) { Text(s.discussionsEntry) }
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))
        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null || viewModel.team == null -> Text(viewModel.error ?: s.teamLoadFailed)
            else -> {
                val team = viewModel.team!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    team.sport?.name?.takeIf { it.isNotBlank() }?.let { Labeled(s.teamSport, it) }
                    team.owner?.let { Labeled(s.teamOwner, fullName(it)) }
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
