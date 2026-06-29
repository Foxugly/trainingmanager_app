package com.foxugly.trainingmanager_app.ui.events

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.data.api.ExerciseDto
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    eventId: Int,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                viewModel.event?.name ?: s.eventsTitle,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))

        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null || viewModel.event == null ->
                Text(viewModel.error ?: s.eventLoadFailed)
            else -> {
                val event = viewModel.event!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val schedule = listOfNotNull(event.date, listOfNotNull(event.hourStart, event.hourEnd).joinToString("–").ifBlank { null }).joinToString(" · ")
                    if (schedule.isNotBlank()) Field(null, schedule)
                    if (event.location.isNotBlank()) Field(s.eventLocation, event.location)
                    event.place?.let { Field(null, listOfNotNull(it.name.ifBlank { null }, it.address.ifBlank { null }).joinToString(" — ")) }
                    event.referProgram?.let { Field(s.eventProgram, it.name) }
                    if (viewModel.showDistance) Field(s.eventDistance, event.total.toString())
                    if (viewModel.showGoal) event.goal?.takeIf { it.isNotBlank() }?.let { Field(s.eventGoal, it) }
                    if (event.equipment.isNotBlank()) Field(s.eventEquipment, event.equipment)

                    if (viewModel.showRounds && event.roundsDetail.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(s.trainingSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        event.roundsDetail.sortedBy { it.order }.forEach { round ->
                            Spacer(Modifier.height(8.dp))
                            val roundTitle = s.roundLabel(round.order) + if (round.count > 1) " ×${round.count}" else ""
                            Text(roundTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            round.exercises.sortedBy { it.order }.forEach { ex -> ExerciseRow(ex) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    viewModel.rsvpError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                    RsvpRow(
                        myStatus = viewModel.rsvp?.myStatus,
                        enabled = !viewModel.isSavingRsvp,
                        goingLabel = s.rsvpGoing,
                        maybeLabel = s.rsvpMaybe,
                        notGoingLabel = s.rsvpNotGoing,
                        onSelect = { status -> scope.launch { viewModel.setRsvp(eventId, status) } },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(s.rotiLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    viewModel.rotiError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { n ->
                            RsvpButton(
                                label = n.toString(),
                                selected = viewModel.rotiScore == n,
                                enabled = !viewModel.isSavingRoti,
                            ) { scope.launch { viewModel.setRoti(eventId, n) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(ex: ExerciseDto) {
    val main = buildString {
        if (ex.repetition > 0) append("${ex.repetition} × ")
        if (ex.distance > 0) append("${ex.distance} m")
    }.trim().ifBlank { ex.notes.ifBlank { "—" } }
    val meta = listOfNotNull(
        ex.modality?.name?.ifBlank { null },
        ex.energysegment?.abv?.ifBlank { null },
        ex.tStart?.let { "▶ $it" },
        ex.tBreak?.let { "⏸ $it" },
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp)) {
        Text(main, style = MaterialTheme.typography.bodyMedium)
        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
        if (ex.notes.isNotBlank() && main != ex.notes) Text(ex.notes, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Field(label: String?, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label != null) Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RsvpRow(
    myStatus: String?,
    enabled: Boolean,
    goingLabel: String,
    maybeLabel: String,
    notGoingLabel: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RsvpButton(goingLabel, selected = myStatus == "going", enabled = enabled) { onSelect("going") }
        RsvpButton(maybeLabel, selected = myStatus == "maybe", enabled = enabled) { onSelect("maybe") }
        RsvpButton(notGoingLabel, selected = myStatus == "not_going", enabled = enabled) { onSelect("not_going") }
    }
}

@Composable
private fun RsvpButton(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(label) }
    }
}
