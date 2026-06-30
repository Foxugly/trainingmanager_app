package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.PasswordField
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    if (viewModel.success) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.cpSuccess, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text(s.back) }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(s.cpTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        viewModel.error?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        PasswordField(s.currentPassword, viewModel.currentPassword) { viewModel.currentPassword = it; viewModel.clearError() }
        Spacer(Modifier.height(12.dp))
        PasswordField(s.newPassword, viewModel.newPassword) { viewModel.newPassword = it; viewModel.clearError() }
        Spacer(Modifier.height(12.dp))
        PasswordField(s.confirmPassword, viewModel.confirmPassword) { viewModel.confirmPassword = it; viewModel.clearError() }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { scope.launch { viewModel.submit() } },
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(s.cpSubmit)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(s.back) }
    }
}
