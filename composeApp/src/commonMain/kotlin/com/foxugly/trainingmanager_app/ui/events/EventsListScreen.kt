package com.foxugly.trainingmanager_app.ui.events

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
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.i18n.LocalStrings

@Composable
fun EventsListScreen(
    viewModel: EventsListViewModel,
    onEventClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.eventsTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))
        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null ->
                Text(viewModel.error!!)
            viewModel.events.isEmpty() ->
                Text(s.eventsEmpty)
            else ->
                LazyColumn(Modifier.fillMaxSize()) {
                    items(viewModel.events, key = { it.id }) { event ->
                        EventRow(event, onClick = { onEventClick(event.id) })
                        HorizontalDivider()
                    }
                }
        }
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    ) {
        Text(event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        val when0 = listOfNotNull(event.date, event.hourStart).joinToString(" · ")
        val where0 = listOfNotNull(event.location?.ifBlank { null }, event.referProgram.name.ifBlank { null }).joinToString(" — ")
        if (when0.isNotBlank()) Text(when0, style = MaterialTheme.typography.bodySmall)
        if (where0.isNotBlank()) Text(where0, style = MaterialTheme.typography.bodySmall)
    }
}
