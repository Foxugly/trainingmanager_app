package com.foxugly.trainingmanager_app.ui.discussions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.foxugly.trainingmanager_app.api.generated.models.Topic
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.EmptyState
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.teams.AddNameDialog
import kotlinx.coroutines.launch

@Composable
fun TopicsListScreen(
    viewModel: TopicsListViewModel,
    teamId: Int,
    onTopicClick: (Topic) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Topic?>(null) }

    DetailScaffold(title = s.discussionsEntry, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null ->
                    ErrorState(viewModel.error!!, onRetry = { scope.launch { viewModel.load(teamId) } }, retryLabel = s.retry)
                else -> Column(Modifier.fillMaxSize()) {
                    viewModel.actionError?.let { ErrorBanner(it, Modifier.padding(16.dp)) }
                    if (viewModel.topics.isEmpty()) {
                        EmptyState(s.topicsEmpty)
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(viewModel.topics, key = { it.id }) { topic ->
                                Card(onClick = { onTopicClick(topic) }, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.weight(1f).padding(16.dp)) {
                                            Text(topic.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                            Text("${topic.messageCount}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (viewModel.canDelete(topic)) {
                                            IconButton(onClick = { confirmDelete = topic }) {
                                                Icon(Icons.Filled.Delete, contentDescription = s.topicDelete)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.canCreateTopic && !viewModel.isLoading && viewModel.error == null) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) { Icon(Icons.Filled.Add, contentDescription = s.newTopic) }
            }
        }
    }

    if (showAdd) {
        AddNameDialog(
            title = s.newTopic,
            label = s.topicTitle,
            onDismiss = { showAdd = false },
            onConfirm = { title -> showAdd = false; scope.launch { viewModel.addTopic(title) } },
        )
    }
    confirmDelete?.let { topic ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(s.topicDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch { viewModel.deleteTopic(topic.id) }
                }) { Text(s.topicDelete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(s.cancel) } },
        )
    }
}
