package com.foxugly.trainingmanager_app.ui.events

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.foxugly.trainingmanager_app.api.generated.models.AttachmentStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.Exercise
import com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.stripHtml
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    eventId: Int,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    DetailScaffold(
        title = viewModel.event?.name ?: s.eventsTitle,
        onBack = onBack,
        actions = {
            if (viewModel.canManage) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = s.edit) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        when {
            viewModel.isLoading -> LoadingState()
            viewModel.error != null || viewModel.event == null ->
                ErrorState(viewModel.error ?: s.eventLoadFailed, onRetry = { scope.launch { viewModel.load(eventId) } }, retryLabel = s.retry)
            else -> {
                val event = viewModel.event!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    val schedule = listOfNotNull(event.date, listOfNotNull(event.hourStart, event.hourEnd).joinToString("–").ifBlank { null }).joinToString(" · ")
                    if (schedule.isNotBlank()) Field(null, schedule)
                    event.location?.takeIf { it.isNotBlank() }?.let { Field(s.eventLocation, it) }
                    event.place?.let { Field(null, listOfNotNull(it.name.ifBlank { null }, it.address.ifBlank { null }).joinToString(" — ")) }
                    event.referProgram.name.takeIf { it.isNotBlank() }?.let { Field(s.eventProgram, it) }
                    if (viewModel.showDistance) Field(s.eventDistance, (event.total ?: 0).toString())
                    if (viewModel.showGoal) event.goal?.takeIf { it.isNotBlank() }?.let { Field(s.eventGoal, stripHtml(it)) }
                    event.equipment?.takeIf { it.isNotBlank() }?.let { Field(s.eventEquipment, stripHtml(it)) }

                    if (viewModel.showRounds && event.roundsDetail.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(s.trainingSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        event.roundsDetail.sortedBy { it.order }.forEach { round ->
                            Spacer(Modifier.height(8.dp))
                            val roundTitle = s.roundLabel(round.order) + if (round.count > 1) " ×${round.count}" else ""
                            Text(roundTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            round.exercises.sortedBy { it.order ?: 0 }.forEach { ex -> ExerciseRow(ex) }
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

                    if (viewModel.attachments.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(s.attachmentsSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        viewModel.attachmentError?.let { Spacer(Modifier.height(8.dp)); ErrorBanner(it) }
                        viewModel.attachments.forEach { att ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(att.filename, style = MaterialTheme.typography.bodyMedium)
                                    Text("${att.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                }
                                if (att.status == AttachmentStatusEnum.READY) {
                                    TextButton(onClick = { scope.launch { viewModel.downloadAttachment(att.id) } }) { Text(s.download) }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ExerciseRow(ex: Exercise) {
    val notes = ex.notes.orEmpty()
    val main = buildString {
        if ((ex.repetition ?: 0) > 0) append("${ex.repetition} × ")
        if ((ex.distance ?: 0) > 0) append("${ex.distance} m")
    }.trim().ifBlank { notes.ifBlank { "—" } }
    val meta = listOfNotNull(
        ex.modality.name.ifBlank { null },
        ex.energysegment.abv.ifBlank { null },
        ex.tStart?.let { "▶ $it" },
        ex.tBreak?.let { "⏸ $it" },
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp)) {
        Text(main, style = MaterialTheme.typography.bodyMedium)
        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
        if (notes.isNotBlank() && main != notes) Text(notes, style = MaterialTheme.typography.bodySmall)
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
    myStatus: RsvpSummary.MyStatus?,
    enabled: Boolean,
    goingLabel: String,
    maybeLabel: String,
    notGoingLabel: String,
    onSelect: (RsvpStatusEnum) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RsvpButton(goingLabel, selected = myStatus == RsvpSummary.MyStatus.GOING, enabled = enabled) { onSelect(RsvpStatusEnum.GOING) }
        RsvpButton(maybeLabel, selected = myStatus == RsvpSummary.MyStatus.MAYBE, enabled = enabled) { onSelect(RsvpStatusEnum.MAYBE) }
        RsvpButton(notGoingLabel, selected = myStatus == RsvpSummary.MyStatus.NOT_GOING, enabled = enabled) { onSelect(RsvpStatusEnum.NOT_GOING) }
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
