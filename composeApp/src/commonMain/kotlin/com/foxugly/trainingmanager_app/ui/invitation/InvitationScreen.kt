package com.foxugly.trainingmanager_app.ui.invitation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

@Composable
fun InvitationScreen(
    viewModel: InvitationViewModel,
    token: String,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(token) { viewModel.load(token) }

    if (viewModel.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(InvitationStrings.loadingTitle)
        }
        return
    }

    val team = viewModel.teamName
    if (viewModel.lookupError != null || team == null) {
        TerminalMessage(
            title = InvitationStrings.alreadyHandledTitle,
            body = viewModel.lookupError ?: InvitationStrings.alreadyHandledBody,
            onBackToLogin = onBackToLogin,
        )
        return
    }
    if (!viewModel.isPending) {
        TerminalMessage(
            title = InvitationStrings.alreadyHandledTitle,
            body = InvitationStrings.alreadyHandledBody,
            onBackToLogin = onBackToLogin,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(InvitationStrings.joinTitle(team), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        viewModel.submitError?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it; viewModel.clearSubmitError() },
            label = { Text(InvitationStrings.password) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.confirmPassword,
            onValueChange = { viewModel.confirmPassword = it; viewModel.clearSubmitError() },
            label = { Text(InvitationStrings.confirmPassword) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { scope.launch { viewModel.accept(token, onSuccess) } },
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(InvitationStrings.join)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBackToLogin) { Text(InvitationStrings.backToLogin) }
    }
}

@Composable
private fun TerminalMessage(title: String, body: String, onBackToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(body)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBackToLogin) { Text(InvitationStrings.backToLogin) }
    }
}
