package com.foxugly.trainingmanager_app.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.api.generated.models.Notification
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.EmptyState
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.MainScaffold
import com.foxugly.trainingmanager_app.ui.components.MainTab
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onSelectTab: (MainTab) -> Unit,
    onOpen: (NotificationTarget) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(
        title = s.notificationsTitle,
        currentTab = MainTab.NOTIFICATIONS,
        onSelectTab = onSelectTab,
        actions = {
            IconButton(onClick = { scope.launch { viewModel.markAllRead() } }) {
                Icon(Icons.Filled.DoneAll, contentDescription = s.markAllRead)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.error != null ->
                    ErrorState(viewModel.error!!, onRetry = { scope.launch { viewModel.load() } }, retryLabel = s.retry)
                viewModel.notifications.isEmpty() -> EmptyState(s.notificationsEmpty)
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(viewModel.notifications, key = { it.id }) { notif ->
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
        if (notif.createdAt.isNotBlank()) Text(notif.createdAt, style = MaterialTheme.typography.labelSmall)
    }
}
