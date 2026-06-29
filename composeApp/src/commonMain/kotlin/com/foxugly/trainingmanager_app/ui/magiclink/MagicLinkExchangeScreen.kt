package com.foxugly.trainingmanager_app.ui.magiclink

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
fun MagicLinkExchangeScreen(
    viewModel: MagicLinkExchangeViewModel,
    token: String,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    LaunchedEffect(token) { viewModel.exchange(token, onSuccess) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (viewModel.state) {
            MagicLinkExchangeViewModel.ExchangeState.Loading,
            MagicLinkExchangeViewModel.ExchangeState.Success -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(MagicLinkStrings.exchangeLoading)
            }
            MagicLinkExchangeViewModel.ExchangeState.Expired -> {
                Text(MagicLinkStrings.expiredTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(MagicLinkStrings.expiredBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(MagicLinkStrings.backToLogin) }
            }
            MagicLinkExchangeViewModel.ExchangeState.Invalid -> {
                Text(MagicLinkStrings.invalidTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(MagicLinkStrings.invalidBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(MagicLinkStrings.backToLogin) }
            }
        }
    }
}
