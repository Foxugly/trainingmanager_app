package com.foxugly.trainingmanager_app.ui.dashboard

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.data.api.DashboardEventItem
import com.foxugly.trainingmanager_app.data.api.DashboardHistoryItem
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    authRepository: AuthRepository,
    onEvents: () -> Unit,
    onProfile: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.dashboardTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onEvents) { Text(s.eventsEntry) }
            TextButton(onClick = onProfile) { Text(s.profileTitle) }
            TextButton(onClick = { scope.launch { authRepository.logout(); onLoggedOut() } }) { Text(s.logout) }
        }
        Spacer(Modifier.height(8.dp))

        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(viewModel.error!!)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { viewModel.load() } }) { Text(s.retry) }
                }
            else -> {
                val summary = viewModel.summary
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(s.dashboardTeams(summary?.memberTeams?.size ?: 0), style = MaterialTheme.typography.titleMedium)
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
