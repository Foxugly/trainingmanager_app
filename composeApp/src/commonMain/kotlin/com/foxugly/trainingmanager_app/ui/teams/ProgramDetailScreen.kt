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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
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
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.stripHtml
import com.foxugly.trainingmanager_app.ui.events.EventsFilter
import kotlinx.coroutines.launch

@Composable
fun ProgramDetailScreen(
    viewModel: ProgramDetailViewModel,
    programId: Int,
    onEventClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(programId) { viewModel.load(programId) }

    DetailScaffold(title = viewModel.program?.name ?: s.programsSection, onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.error != null || viewModel.program == null ->
                ErrorState(
                    viewModel.error ?: s.programLoadFailed,
                    modifier = Modifier.padding(padding),
                    onRetry = { scope.launch { viewModel.load(programId) } },
                    retryLabel = s.retry,
                )
            else -> {
                val p = viewModel.program!!
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    val dates = listOfNotNull(p.dateStart, p.dateEnd).joinToString(" → ")
                    if (dates.isNotBlank()) ProgramField(s.programDatesLabel, dates)
                    p.frequencyPerWeek?.let { ProgramField(s.programFrequencyLabel, it.toString()) }
                    p.description?.takeIf { it.isNotBlank() }?.let { ProgramField(s.programDescriptionField, stripHtml(it)) }

                    Spacer(Modifier.height(16.dp))
                    Text(s.programEventsLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewModel.filter == EventsFilter.UPCOMING,
                            onClick = { scope.launch { viewModel.setFilter(EventsFilter.UPCOMING) } },
                            label = { Text(s.eventsFilterUpcoming) },
                        )
                        FilterChip(
                            selected = viewModel.filter == EventsFilter.PAST,
                            onClick = { scope.launch { viewModel.setFilter(EventsFilter.PAST) } },
                            label = { Text(s.eventsFilterPast) },
                        )
                        FilterChip(
                            selected = viewModel.filter == EventsFilter.ALL,
                            onClick = { scope.launch { viewModel.setFilter(EventsFilter.ALL) } },
                            label = { Text(s.eventsFilterAll) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (viewModel.events.isEmpty()) {
                        Text(s.eventsEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        viewModel.events.forEach { e ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onEventClick(e.id) }.padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    val schedule = listOfNotNull(e.date, listOfNotNull(e.hourStart, e.hourEnd).joinToString("–").ifBlank { null }).joinToString(" · ")
                                    if (schedule.isNotBlank()) Text(schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
