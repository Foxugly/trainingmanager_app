package com.foxugly.trainingmanager_app.ui.events

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
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.EmptyState
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.MainScaffold
import com.foxugly.trainingmanager_app.ui.components.MainTab
import kotlinx.coroutines.launch

@Composable
fun EventsListScreen(
    viewModel: EventsListViewModel,
    onSelectTab: (MainTab) -> Unit,
    onEventClick: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(title = s.eventsTitle, currentTab = MainTab.EVENTS, onSelectTab = onSelectTab) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null ->
                    ErrorState(viewModel.error!!, onRetry = { scope.launch { viewModel.load() } }, retryLabel = s.retry)
                viewModel.events.isEmpty() ->
                    EmptyState(s.eventsEmpty)
                else ->
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(viewModel.events, key = { it.id }) { event ->
                            EventRow(event, onClick = { onEventClick(event.id) })
                        }
                    }
            }
        }
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        val when0 = listOfNotNull(event.date, event.hourStart).joinToString(" · ")
        val where0 = listOfNotNull(event.location?.ifBlank { null }, event.referProgram.name.ifBlank { null }).joinToString(" — ")
            if (when0.isNotBlank()) Text(when0, style = MaterialTheme.typography.bodySmall)
            if (where0.isNotBlank()) Text(where0, style = MaterialTheme.typography.bodySmall)
        }
    }
}
