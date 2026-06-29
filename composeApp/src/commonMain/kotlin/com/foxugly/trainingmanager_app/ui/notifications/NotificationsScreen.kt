package com.foxugly.trainingmanager_app.ui.notifications

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.data.api.Notification
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpen: (NotificationTarget) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.notificationsTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { viewModel.markAllRead() } }) { Text(s.markAllRead) }
            TextButton(onClick = onBack) { Text(s.back) }
        }
        Spacer(Modifier.height(8.dp))
        when {
            viewModel.isLoading ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            viewModel.error != null -> Text(viewModel.error!!)
            viewModel.notifications.isEmpty() -> Text(s.notificationsEmpty)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(viewModel.notifications) { notif ->
                    NotificationRow(notif) {
                        scope.launch { viewModel.markRead(notif.id) }
                        parseNotificationTarget(notif.url)?.let(onOpen)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: Notification, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(
            notif.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
        )
        if (notif.body.isNotBlank()) Text(notif.body, style = MaterialTheme.typography.bodyMedium)
        notif.createdAt?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}
