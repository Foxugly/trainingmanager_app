package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmailConfirmScreen(
    viewModel: EmailConfirmViewModel,
    key: String,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    LaunchedEffect(key) { viewModel.confirm(key, onSuccess) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (viewModel.state) {
            EmailConfirmViewModel.State.Loading,
            EmailConfirmViewModel.State.Success -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(ConfirmStrings.emailConfirmLoading)
            }
            EmailConfirmViewModel.State.Invalid -> {
                Text(ConfirmStrings.emailInvalidTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(ConfirmStrings.emailInvalidBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(ConfirmStrings.backToLogin) }
            }
        }
    }
}
