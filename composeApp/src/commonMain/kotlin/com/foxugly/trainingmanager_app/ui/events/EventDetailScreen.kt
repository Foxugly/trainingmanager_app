package com.foxugly.trainingmanager_app.ui.events

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.AttachmentStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.api.generated.models.Exercise
import com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.i18n.Strings
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.stripHtml
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    attendanceViewModel: AttendanceViewModel,
    eventId: Int,
    onEdit: () -> Unit,
    onEditTraining: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    val attachmentPicker = rememberFilePickerLauncher { file ->
        file?.let { pf -> scope.launch { viewModel.uploadAttachment(pf.name, mimeType(pf.name), pf.readBytes()) } }
    }

    var selectedTab by remember { mutableStateOf(0) }

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
                    Column(Modifier.fillMaxSize()) {
                        // Compact header: date · time, program, distance, location, goal.
                        EventInfoHeader(event, viewModel, s)

                        val tabs = listOf(
                            s.eventTabTraining,
                            s.eventTabAttendance,
                            s.eventTabRoti,
                            s.eventTabAttachments,
                            s.eventTabNotes,
                        )
                        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                            tabs.forEachIndexed { i, title ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                            }
                        }

                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        ) {
                            when (selectedTab) {
                                0 -> TrainingTab(event, viewModel, s, onEditTraining)
                                1 -> AttendanceTab(viewModel, attendanceViewModel, s, eventId, scope)
                                2 -> RotiTab(viewModel, s, eventId, scope)
                                3 -> AttachmentsTab(viewModel, s, scope) { attachmentPicker.launch() }
                                else -> NotesVisibilityTab(event, s)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventInfoHeader(event: Event, viewModel: EventDetailViewModel, s: Strings) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val schedule = listOfNotNull(
            event.date,
            listOfNotNull(event.hourStart, event.hourEnd).joinToString("–").ifBlank { null },
        ).joinToString(" · ")
        if (schedule.isNotBlank()) {
            Text(schedule, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        event.referProgram.name.takeIf { it.isNotBlank() }?.let { CompactField(s.eventProgram, it) }
        if (viewModel.showDistance) CompactField(s.eventDistance, (event.total ?: 0).toString())
        val place = event.place?.let { p ->
            listOfNotNull(p.name.ifBlank { null }, p.address.ifBlank { null }).joinToString(" — ").ifBlank { null }
        }
        val location = listOfNotNull(event.location?.takeIf { it.isNotBlank() }, place).joinToString(" · ")
        if (location.isNotBlank()) {
            CompactTappable(s.eventLocation, location) {
                viewModel.openMap(event.place?.address?.ifBlank { event.location } ?: event.location.orEmpty())
            }
        }
        if (viewModel.showGoal) event.goal?.takeIf { it.isNotBlank() }?.let { CompactField(s.eventGoal, stripHtml(it)) }
    }
}

@Composable
private fun TrainingTab(event: Event, viewModel: EventDetailViewModel, s: Strings, onEditTraining: () -> Unit) {
    event.equipment?.takeIf { it.isNotBlank() }?.let {
        Field(s.eventEquipment, stripHtml(it))
        Spacer(Modifier.height(8.dp))
    }
    if (viewModel.showRounds && event.roundsDetail.isNotEmpty()) {
        event.roundsDetail.sortedBy { it.order }.forEach { round ->
            val roundTitle = s.roundLabel(round.order) + if (round.count > 1) " ×${round.count}" else ""
            Text(roundTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            round.exercises.sortedBy { it.order ?: 0 }.forEach { ex -> ExerciseRow(ex) }
            Spacer(Modifier.height(8.dp))
        }
    } else {
        Text(s.trainingEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (viewModel.canManage) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onEditTraining, modifier = Modifier.fillMaxWidth()) { Text(s.trainingEditEntry) }
    }
}

@Composable
private fun AttendanceTab(
    viewModel: EventDetailViewModel,
    attendanceViewModel: AttendanceViewModel,
    s: Strings,
    eventId: Int,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    viewModel.rsvpError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
    RsvpRow(
        myStatus = viewModel.rsvp?.myStatus,
        enabled = !viewModel.isSavingRsvp,
        goingLabel = s.rsvpGoing,
        maybeLabel = s.rsvpMaybe,
        notGoingLabel = s.rsvpNotGoing,
        onSelect = { status -> scope.launch { viewModel.setRsvp(eventId, status) } },
    )
    if (viewModel.canManage) {
        Spacer(Modifier.height(16.dp))
        Text(s.eventManageAttendance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        AttendanceContent(attendanceViewModel, eventId)
    }
}

@Composable
private fun RotiTab(viewModel: EventDetailViewModel, s: Strings, eventId: Int, scope: kotlinx.coroutines.CoroutineScope) {
    Text(s.rotiLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Text(s.rotiScaleHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    viewModel.rotiError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
    val rotiEmojis = listOf("😞", "🙁", "😐", "🙂", "😄")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { n ->
            RsvpButton(label = rotiEmojis[n - 1], selected = viewModel.rotiScore == n, enabled = !viewModel.isSavingRoti) {
                scope.launch { viewModel.setRoti(eventId, n) }
            }
        }
    }
    if (viewModel.canManage) {
        viewModel.rotiSummary?.takeIf { it.count > 0 }?.let { sum ->
            Spacer(Modifier.height(16.dp))
            Text(s.rotiSummaryTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (1..5).forEach { n ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(rotiEmojis[n - 1])
                        Text("${sum.distribution[n.toString()] ?: 0}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            sum.average?.let { avg ->
                Text(
                    "⌀ ${(avg * 10).roundToInt() / 10.0}/5 · ${sum.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AttachmentsTab(
    viewModel: EventDetailViewModel,
    s: Strings,
    scope: kotlinx.coroutines.CoroutineScope,
    onPick: () -> Unit,
) {
    viewModel.attachmentError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
    if (viewModel.attachments.isEmpty()) {
        Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    viewModel.attachments.forEach { att ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(att.filename, style = MaterialTheme.typography.bodyMedium)
                Text("${att.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            }
            if (att.status == AttachmentStatusEnum.READY) {
                TextButton(onClick = { scope.launch { viewModel.downloadAttachment(att.id) } }) { Text(s.download) }
            }
            if (viewModel.canManage) {
                IconButton(onClick = { scope.launch { viewModel.deleteAttachment(att.id) } }) {
                    Icon(Icons.Filled.Delete, contentDescription = s.attachmentDelete)
                }
            }
        }
    }
    if (viewModel.canManage) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPick,
            enabled = !viewModel.isUploadingAttachment,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (viewModel.isUploadingAttachment) s.attachmentUploading else s.attachmentAdd) }
    }
}

@Composable
private fun NotesVisibilityTab(event: Event, s: Strings) {
    val debrief = event.debrief?.let { stripHtml(it) }?.takeIf { it.isNotBlank() }
    Field(s.eventFieldDebrief, debrief ?: "—")
    Spacer(Modifier.height(12.dp))
    Text(s.eventVisSection, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    CompactField(s.visShowDistance, visLabel(event.visDistance, s))
    CompactField(s.visShowGoal, visLabel(event.visGoal, s))
    CompactField(s.visShowRounds, visLabel(event.visRounds, s))
}

private fun visLabel(mode: VisibilityMode?, s: Strings): String = when (mode) {
    VisibilityMode.ALWAYS -> s.visAlways
    VisibilityMode.AFTER -> s.visAfter
    VisibilityMode.NEVER -> s.visNever
    null -> "—"
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

/** Dense label + value on one line, for the compact info header. */
@Composable
private fun CompactField(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

/** Compact tappable line (location → maps app). */
@Composable
private fun CompactTappable(label: String, value: String, onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 2.dp),
    ) {
        Text("$label : ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

/** Best-effort MIME type from a filename extension (backend enforces its own allow-list). */
private fun mimeType(filename: String): String = when (filename.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "heic" -> "image/heic"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "csv" -> "text/csv"
    "txt" -> "text/plain"
    "mp4" -> "video/mp4"
    "mov" -> "video/quicktime"
    "zip" -> "application/zip"
    else -> "application/octet-stream"
}
