package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.stripHtml
import kotlinx.coroutines.launch

@Composable
fun ProgramDetailScreen(
    viewModel: ProgramDetailViewModel,
    programId: Int,
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
                    ProgramField(s.programEventsLabel, p.events.size.toString())
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
