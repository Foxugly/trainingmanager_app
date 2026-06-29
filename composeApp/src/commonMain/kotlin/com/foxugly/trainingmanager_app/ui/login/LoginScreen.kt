package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.PasswordHiddenIcon
import com.foxugly.trainingmanager_app.ui.components.PasswordVisibleIcon
import kotlinx.coroutines.launch

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

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it; viewModel.clearError() },
            label = { Text(s.password) },
            visualTransformation = if (viewModel.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { viewModel.passwordVisible = !viewModel.passwordVisible }) {
                    Icon(
                        imageVector = if (viewModel.passwordVisible) PasswordHiddenIcon else PasswordVisibleIcon,
                        contentDescription = if (viewModel.passwordVisible) s.hidePassword else s.showPassword,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
}
