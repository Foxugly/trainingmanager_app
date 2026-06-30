package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.i18n.LocalStrings

@Composable
fun TeamsListScreen(
    viewModel: TeamsListViewModel,
    onTeamClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.teamsTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))
        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null -> Text(viewModel.error!!)
            viewModel.teams.isEmpty() -> Text(s.teamsEmpty)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(viewModel.teams, key = { it.id }) { team ->
                    TeamRow(team) { onTeamClick(team.id) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TeamRow(team: Team, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(team.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        team.sport.name.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
