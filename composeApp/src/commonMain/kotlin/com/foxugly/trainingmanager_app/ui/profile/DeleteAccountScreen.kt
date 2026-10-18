package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.DetailScaffold
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.PasswordField
import kotlinx.coroutines.launch

/** Profile > "Delete my account": explanation, current password, then a final confirmation dialog. */
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    var confirmOpen by remember { mutableStateOf(false) }

    DetailScaffold(title = s.deleteAccountCta, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            ) {
                Text(s.deleteAccountTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text(s.deleteAccountBody)
                Spacer(Modifier.height(16.dp))
                viewModel.error?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }
                PasswordField(s.currentPassword, viewModel.currentPassword) {
                    viewModel.currentPassword = it
                    viewModel.clearError()
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { confirmOpen = true },
                    enabled = viewModel.canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text(s.deleteAccountConfirm)
                    }
                }
            }
        }
    }

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text(s.deleteAccountTitle) },
            text = { Text(s.deleteAccountBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    scope.launch { if (viewModel.submit()) onDeleted() }
                }) { Text(s.deleteAccountConfirm, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpen = false }) { Text(s.cancel) }
            },
        )
    }
}
