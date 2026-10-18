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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.ui.components.LinkSignInConfirmation
import kotlinx.coroutines.launch

@Composable
fun MagicLinkExchangeScreen(
    viewModel: MagicLinkExchangeViewModel,
    token: String,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    onCancel: () -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(token) { viewModel.exchange(token) }

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
                Text(s.magicExchangeLoading)
            }
            MagicLinkExchangeViewModel.ExchangeState.AwaitingConfirmation ->
                LinkSignInConfirmation(
                    targetEmail = viewModel.targetEmail.orEmpty(),
                    currentEmail = viewModel.currentEmail,
                    onConfirm = { scope.launch { viewModel.confirm(onSuccess) } },
                    onCancel = { viewModel.cancel(); onCancel() },
                )
            MagicLinkExchangeViewModel.ExchangeState.Expired -> {
                Text(s.magicExpiredTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(s.magicExpiredBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(s.backToLogin) }
            }
            MagicLinkExchangeViewModel.ExchangeState.Invalid -> {
                Text(s.magicInvalidTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(s.magicInvalidBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(s.backToLogin) }
            }
        }
    }
}
