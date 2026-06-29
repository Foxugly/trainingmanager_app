package com.foxugly.trainingmanager_app.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.platform.TurnstileWebView
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    if (viewModel.registered) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(s.registerCheckEmailTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(s.registerCheckEmailBody, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(viewModel.email, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text(s.backToLogin) }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(s.registerTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        viewModel.error?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }

        OutlinedTextField(
            value = viewModel.firstName,
            onValueChange = { viewModel.firstName = it; viewModel.clearError() },
            label = { Text(s.registerFirstName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.lastName,
            onValueChange = { viewModel.lastName = it; viewModel.clearError() },
            label = { Text(s.registerLastName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it; viewModel.clearError() },
            label = { Text(s.email) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it; viewModel.clearError() },
            label = { Text(s.registerPassword) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = viewModel.password.isNotEmpty() && !viewModel.passwordValid,
            supportingText = {
                if (viewModel.password.isNotEmpty() && !viewModel.passwordValid) {
                    Text(s.registerPasswordTooShort)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text(s.turnstilePrompt, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        TurnstileWebView(
            onToken = { viewModel.onTurnstileToken(it) },
            onError = { viewModel.onTurnstileError() },
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { scope.launch { viewModel.submit() } },
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(s.registerSubmit)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(s.backToLogin) }
    }
}
