package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.CustomUserPublic
import com.foxugly.trainingmanager_app.api.generated.models.Equipment
import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.i18n.languageDisplayNames
import com.foxugly.trainingmanager_app.i18n.supportedLanguages
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
    onProgramClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    DetailScaffold(
        title = viewModel.team?.name ?: s.teamsTitle,
        onBack = onBack,
        actions = {
            if (viewModel.canManage) {
                IconButton(onClick = onCreateEvent) {
                    Icon(Icons.Filled.Add, contentDescription = s.addEvent)
                }
            }
            IconButton(onClick = onDiscussions) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = s.discussionsEntry)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null || viewModel.team == null ->
                    ErrorState(viewModel.error ?: s.teamLoadFailed, onRetry = { scope.launch { viewModel.load(teamId) } }, retryLabel = s.retry)
                else -> {
                    val team = viewModel.team!!
                    var selectedTab by remember { mutableStateOf(0) }
                    val tabs = listOf(
                        s.teamTabInfos,
                        s.programsSection,
                        s.teamTabMembers,
                        s.teamTabPlaces,
                        s.teamTabEquipment,
                    )
                    Column(Modifier.fillMaxSize()) {
                        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                            tabs.forEachIndexed { i, title ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                            }
                        }
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            when (selectedTab) {
                                0 -> {
                                    var editing by remember { mutableStateOf(false) }
                                    if (editing) {
                                        var name by remember { mutableStateOf(team.name) }
                                        var lang by remember { mutableStateOf(team.language) }
                                        var pub by remember { mutableStateOf(team.isPublic == true) }
                                        var roti by remember { mutableStateOf(team.rotiEnabled == true) }
                                        var rsvp by remember { mutableStateOf(team.rsvpEnabled == true) }
                                        viewModel.teamError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = { name = it },
                                            label = { Text(s.teamName) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        LanguagePicker(lang) { lang = it }
                                        SwitchRow(s.teamPublic, pub) { pub = it }
                                        SwitchRow(s.teamRotiEnabled, roti) { roti = it }
                                        SwitchRow(s.teamRsvpEnabled, rsvp) { rsvp = it }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { editing = false }) { Text(s.cancel) }
                                            Button(
                                                onClick = { scope.launch { viewModel.saveInfos(name, lang, pub, roti, rsvp); editing = false } },
                                                enabled = !viewModel.isSavingTeam && name.isNotBlank(),
                                            ) { Text(s.save) }
                                        }
                                    } else {
                                        if (viewModel.canManage) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = s.edit) }
                                            }
                                        }
                                        team.sport.name.takeIf { it.isNotBlank() }?.let { Labeled(s.teamSport, it) }
                                        team.level?.name?.takeIf { it.isNotBlank() }?.let { Labeled(s.teamLevel, it) }
                                        Labeled(s.teamOwner, fullName(team.owner))
                                        if (team.managers.isNotEmpty()) {
                                            Labeled(s.teamManagers, team.managers.joinToString(", ") { fullName(it) })
                                        }
                                        team.language?.let { Labeled(s.languageLabel, languageDisplayNames[it.value] ?: it.value) }
                                        Labeled(s.teamPublic, if (team.isPublic == true) s.yes else s.no)
                                        Labeled(s.teamRotiEnabled, if (team.rotiEnabled == true) s.yes else s.no)
                                        Labeled(s.teamRsvpEnabled, if (team.rsvpEnabled == true) s.yes else s.no)
                                    }
                                }
                                1 -> {
                                    viewModel.programError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                                    if (viewModel.programs.isEmpty()) {
                                        Text(s.programsEmpty, style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        viewModel.programs.forEach { p ->
                                            val active = p.isActive == true
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().clickable { onProgramClick(p.id) }.padding(vertical = 8.dp),
                                            ) {
                                                Text(
                                                    p.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (active) {
                                                    Text(
                                                        s.programActive,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 8.dp),
                                                    )
                                                }
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                    if (viewModel.canManage) {
                                        var showAddProgram by remember { mutableStateOf(false) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { showAddProgram = true }, enabled = !viewModel.isSavingProgram, modifier = Modifier.fillMaxWidth()) { Text(s.addProgram) }
                                        if (showAddProgram) {
                                            AddNameDialog(title = s.addProgram, label = s.programName, onDismiss = { showAddProgram = false }, onConfirm = { name -> showAddProgram = false; scope.launch { viewModel.addProgram(name) } })
                                        }
                                    }
                                }
                                2 -> {
                                    Labeled(s.teamOwner, fullName(team.owner))
                                    if (team.managers.isNotEmpty()) Labeled(s.teamManagers, team.managers.joinToString(", ") { fullName(it) })
                                    Labeled(
                                        s.teamMembers,
                                        if (viewModel.members.isEmpty()) "—" else viewModel.members.joinToString(", ") { it.fullname.ifBlank { listOf(it.firstname, it.lastname).filter { p -> p.isNotBlank() }.joinToString(" ") } },
                                    )
                                    if (viewModel.canManage) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(s.invitationsSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        viewModel.inviteError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                                        if (viewModel.invitations.isEmpty()) {
                                            Text(s.invitationsEmpty, style = MaterialTheme.typography.bodyMedium)
                                        } else {
                                            viewModel.invitations.forEach { inv ->
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                    Text(inv.email, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                    TextButton(onClick = { scope.launch { viewModel.cancelInvitation(inv.id) } }, enabled = !viewModel.isSavingInvite) { Text(s.cancel) }
                                                }
                                            }
                                        }
                                        var showInvite by remember { mutableStateOf(false) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { showInvite = true }, enabled = !viewModel.isSavingInvite, modifier = Modifier.fillMaxWidth()) { Text(s.inviteButton) }
                                        if (showInvite) {
                                            InviteDialog(onDismiss = { showInvite = false }, onConfirm = { email, first, last -> showInvite = false; scope.launch { viewModel.invite(email, first, last) } })
                                        }
                                    }
                                }
                                3 -> {
                                    viewModel.placesError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                                    Text(s.teamPlacesSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    if (team.places.isEmpty()) {
                                        Text("—", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        team.places.forEach { p ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Text(listOfNotNull(p.name.ifBlank { null }, p.address.ifBlank { null }).joinToString(" — "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                if (viewModel.canManage) {
                                                    IconButton(onClick = { scope.launch { viewModel.removePlace(p.id) } }) {
                                                        Icon(Icons.Filled.Delete, contentDescription = s.cancel)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (viewModel.canManage) {
                                        var showAddPlace by remember { mutableStateOf(false) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { showAddPlace = true }, modifier = Modifier.fillMaxWidth()) { Text(s.addPlace) }
                                        if (showAddPlace) {
                                            PlaceDialog(
                                                onDismiss = { showAddPlace = false },
                                                onConfirm = { n, a -> showAddPlace = false; scope.launch { viewModel.addPlace(n, a) } },
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(s.teamSlotsSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    if (viewModel.slots.isEmpty()) {
                                        Text("—", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        viewModel.slots.forEach { slot ->
                                            val place = slot.place?.name?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Text("${s.weekdayName(slot.weekday.value)} ${slot.hourStart}–${slot.hourEnd}$place", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                if (viewModel.canManage) {
                                                    IconButton(onClick = { scope.launch { viewModel.removeSlot(slot.id) } }) {
                                                        Icon(Icons.Filled.Delete, contentDescription = s.cancel)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (viewModel.canManage) {
                                        var showAddSlot by remember { mutableStateOf(false) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { showAddSlot = true }, modifier = Modifier.fillMaxWidth()) { Text(s.addSlot) }
                                        if (showAddSlot) {
                                            SlotDialog(
                                                onDismiss = { showAddSlot = false },
                                                onConfirm = { wd, hs, he -> showAddSlot = false; scope.launch { viewModel.addSlot(wd, hs, he) } },
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    viewModel.equipmentError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
                                    if (team.equipment.isEmpty()) {
                                        Text("—", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        team.equipment.forEach { e ->
                                            Text(e.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                                        }
                                    }
                                    if (viewModel.canManage) {
                                        var showManage by remember { mutableStateOf(false) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showManage = true; scope.launch { viewModel.loadEquipmentCatalog() } },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(s.equipmentManage) }
                                        if (showManage) {
                                            EquipmentDialog(
                                                catalog = viewModel.equipmentCatalog,
                                                selectedIds = team.equipment.map { it.id }.toSet(),
                                                onDismiss = { showManage = false },
                                                onConfirm = { ids -> showManage = false; scope.launch { viewModel.setEquipment(ids) } },
                                            )
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

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LanguagePicker(selected: LanguageEnum?, onSelect: (LanguageEnum) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(s.languageLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.let { languageDisplayNames[it.value] ?: it.value } ?: "—")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { code ->
                DropdownMenuItem(text = { Text(languageDisplayNames[code] ?: code) }, onClick = {
                    expanded = false
                    LanguageEnum.decode(code)?.let(onSelect)
                })
            }
        }
    }
}

@Composable
private fun PlaceDialog(onDismiss: () -> Unit, onConfirm: (name: String, address: String) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addPlace) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text(s.placeName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(address, { address = it }, label = { Text(s.placeAddress) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, address) }, enabled = name.isNotBlank()) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun SlotDialog(onDismiss: () -> Unit, onConfirm: (weekday: Int, hourStart: String, hourEnd: String) -> Unit) {
    val s = LocalStrings.current
    var weekday by remember { mutableStateOf(0) }
    var start by remember { mutableStateOf("18:00") }
    var end by remember { mutableStateOf("19:00") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addSlot) },
        text = {
            Column {
                OutlinedButton(onClick = { expanded = true }) { Text(s.weekdayName(weekday)) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (0..6).forEach { d -> DropdownMenuItem(text = { Text(s.weekdayName(d)) }, onClick = { weekday = d; expanded = false }) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(start, { start = it }, label = { Text(s.eventFieldHourStart) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(end, { end = it }, label = { Text(s.eventFieldHourEnd) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(weekday, start, end) }, enabled = start.isNotBlank() && end.isNotBlank()) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun EquipmentDialog(catalog: List<Equipment>, selectedIds: Set<Int>, onDismiss: () -> Unit, onConfirm: (List<Int>) -> Unit) {
    val s = LocalStrings.current
    val selected = remember { mutableStateListOf<Int>().apply { addAll(selectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.equipmentManage) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (catalog.isEmpty()) Text("—", style = MaterialTheme.typography.bodyMedium)
                catalog.forEach { e ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (selected.contains(e.id)) selected.remove(e.id) else selected.add(e.id)
                        },
                    ) {
                        Checkbox(
                            checked = selected.contains(e.id),
                            onCheckedChange = { c -> if (c) { if (!selected.contains(e.id)) selected.add(e.id) } else selected.remove(e.id) },
                        )
                        Text(e.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.toList()) }) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
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

@Composable
private fun InviteDialog(onDismiss: () -> Unit, onConfirm: (email: String, firstname: String, lastname: String) -> Unit) {
    val s = LocalStrings.current
    var email by remember { mutableStateOf("") }
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.inviteButton) },
        text = {
            Column {
                OutlinedTextField(email, { email = it }, label = { Text(s.email) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(firstname, { firstname = it }, label = { Text(s.firstName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(lastname, { lastname = it }, label = { Text(s.lastName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (email.isNotBlank()) onConfirm(email, firstname, lastname) }, enabled = email.isNotBlank()) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
