package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.PasswordField
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import trainingmanager_app.composeapp.generated.resources.Res
import trainingmanager_app.composeapp.generated.resources.foxugly_logo

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onMagicLink: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(s.loginTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            viewModel.error?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it; viewModel.clearError() },
                label = { Text(s.email) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            PasswordField(
                label = s.password,
                value = viewModel.password,
                onValueChange = { viewModel.password = it; viewModel.clearError() },
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.rememberMe,
                    onCheckedChange = { viewModel.rememberMe = it },
                )
                Text(s.rememberMe)
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { scope.launch { viewModel.submit(onLoginSuccess) } },
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
                    Text(s.loginAction)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onMagicLink, modifier = Modifier.fillMaxWidth()) {
                Text(s.signInByEmailLink)
            }
            TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
                Text(s.forgotPasswordLink)
            }
            TextButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
                Text(s.createAccount)
            }
        }

        // "par [logo] Foxugly" credit pinned to the bottom (mirrors PushIT_app).
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = s.credit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Image(
                painter = painterResource(Res.drawable.foxugly_logo),
                contentDescription = null,
                modifier = Modifier.height(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Foxugly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
