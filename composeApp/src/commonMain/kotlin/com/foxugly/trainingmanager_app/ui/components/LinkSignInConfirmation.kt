package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.i18n.LocalStrings

/**
 * "Sign in as <target>?" step shown before a link (magic link, e-mail
 * confirmation) replaces the session: names the target account and, when
 * someone is already signed in, the account that would be signed out.
 */
@Composable
fun LinkSignInConfirmation(
    targetEmail: String,
    currentEmail: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val s = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.linkSignInTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(s.linkSignInAs(targetEmail), textAlign = TextAlign.Center)
        if (currentEmail != null && !currentEmail.equals(targetEmail, ignoreCase = true)) {
            Spacer(Modifier.height(12.dp))
            Text(
                s.linkSignInReplaces(currentEmail),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(s.linkSignInWarning, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel) { Text(s.cancel) }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onConfirm) { Text(s.linkSignInConfirm) }
        }
    }
}
