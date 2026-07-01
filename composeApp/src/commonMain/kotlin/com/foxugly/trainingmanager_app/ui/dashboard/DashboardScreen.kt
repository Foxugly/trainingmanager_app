package com.foxugly.trainingmanager_app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.DashboardEventItem
import com.foxugly.trainingmanager_app.api.generated.models.DashboardHistoryItem
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.MainScaffold
import com.foxugly.trainingmanager_app.ui.components.MainTab
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSelectTab: (MainTab) -> Unit,
    onTeamClick: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(title = s.dashboardTitle, currentTab = MainTab.DASHBOARD, onSelectTab = onSelectTab) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null ->
                    ErrorState(viewModel.error!!, onRetry = { scope.launch { viewModel.load() } }, retryLabel = s.retry)
                else -> {
                    val summary = viewModel.summary
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Text(
                            s.dashboardTeams(viewModel.teams.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (viewModel.teams.isEmpty()) {
                            Text(s.teamsEmpty, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            viewModel.teams.forEach { team ->
                                TeamLinkCard(
                                    name = team.name,
                                    sport = team.sport.name.takeIf { it.isNotBlank() },
                                    onClick = { onTeamClick(team.id) },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        Text(s.dashboardUpcoming, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val upcoming = summary?.memberUpcoming.orEmpty()
                        if (upcoming.isEmpty()) {
                            Text(s.dashboardNoUpcoming, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            upcoming.forEach { EventRow(it); HorizontalDivider() }
                        }
                        Spacer(Modifier.height(16.dp))

                        Text(s.dashboardHistory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val history = summary?.memberAttendanceHistory.orEmpty()
                        if (history.isEmpty()) {
                            Text(s.dashboardNoHistory, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            history.forEach { HistoryRow(it); HorizontalDivider() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamLinkCard(name: String, sport: String?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                sport?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventRow(item: DashboardEventItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(item.event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        val when0 = listOfNotNull(item.event.date, item.event.hourStart).joinToString(" · ")
        val where0 = listOfNotNull(item.event.location.ifBlank { null }, item.teamName.ifBlank { null }, item.programName).joinToString(" — ")
        if (when0.isNotBlank()) Text(when0, style = MaterialTheme.typography.bodySmall)
        if (where0.isNotBlank()) Text(where0, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryRow(item: DashboardHistoryItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(item.event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        val line = listOfNotNull(item.event.date, item.teamName.ifBlank { null }, item.statusCode.ifBlank { null }).joinToString(" — ")
        if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall)
    }
}
