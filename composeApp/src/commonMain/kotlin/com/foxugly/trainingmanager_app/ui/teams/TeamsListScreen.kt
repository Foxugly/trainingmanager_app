package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.EmptyState
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.MainScaffold
import com.foxugly.trainingmanager_app.ui.components.MainTab
import kotlinx.coroutines.launch

@Composable
fun TeamsListScreen(
    viewModel: TeamsListViewModel,
    onSelectTab: (MainTab) -> Unit,
    onTeamClick: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(title = s.teamsTitle, currentTab = MainTab.TEAMS, onSelectTab = onSelectTab) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null ->
                    ErrorState(viewModel.error!!, onRetry = { scope.launch { viewModel.load() } }, retryLabel = s.retry)
                viewModel.teams.isEmpty() -> EmptyState(s.teamsEmpty)
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.teams, key = { it.id }) { team ->
                        TeamRow(team) { onTeamClick(team.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamRow(team: Team, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(team.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            team.sport.name.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
