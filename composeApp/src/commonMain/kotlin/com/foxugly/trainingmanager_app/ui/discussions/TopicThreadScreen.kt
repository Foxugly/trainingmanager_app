package com.foxugly.trainingmanager_app.ui.discussions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.CustomUserPublic
import com.foxugly.trainingmanager_app.api.generated.models.TopicMessage
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import kotlinx.coroutines.launch

/** Strip the server's sanitized HTML to plain text for display (no HTML renderer needed). */
internal fun stripHtml(s: String): String =
    s.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'")
        .trim()

@Composable
fun TopicThreadScreen(
    viewModel: TopicThreadViewModel,
    teamId: Int,
    topicId: Int,
    allowReplies: Boolean,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(teamId, topicId) { viewModel.load(teamId, topicId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.discussionsEntry, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))
        when {
            viewModel.isLoading -> LoadingState(Modifier.weight(1f))
            viewModel.error != null ->
                ErrorState(
                    viewModel.error!!,
                    modifier = Modifier.weight(1f),
                    onRetry = { scope.launch { viewModel.load(teamId, topicId) } },
                    retryLabel = s.retry,
                )
            else ->
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(viewModel.messages, key = { it.id }) { msg ->
                        MessageRow(
                            msg = msg,
                            mine = viewModel.isMine(msg),
                            editedLabel = s.editedLabel,
                            deleteLabel = s.deleteMessage,
                            onDelete = { scope.launch { viewModel.delete(teamId, topicId, msg.id) } },
                        )
                        HorizontalDivider()
                    }
                }
        }

        if (allowReplies) {
            viewModel.sendError?.let { Spacer(Modifier.height(4.dp)); ErrorBanner(it) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(
                    value = viewModel.composeText,
                    onValueChange = { viewModel.composeText = it },
                    placeholder = { Text(s.messagePlaceholder) },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { scope.launch { viewModel.send(teamId, topicId) } },
                    enabled = !viewModel.isSending && viewModel.composeText.isNotBlank(),
                ) { Text(s.sendMessage) }
            }
        }
    }
}

@Composable
private fun MessageRow(
    msg: TopicMessage,
    mine: Boolean,
    editedLabel: String,
    deleteLabel: String,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(authorName(msg.author), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (msg.editedAt != null) Text(editedLabel, style = MaterialTheme.typography.labelSmall)
            if (mine) TextButton(onClick = onDelete) { Text(deleteLabel) }
        }
        Text(stripHtml(msg.content), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun authorName(u: CustomUserPublic?): String =
    u?.let { listOf(it.firstName, it.lastName).filter { p -> p.isNotBlank() }.joinToString(" ") }?.ifBlank { "—" } ?: "—"
