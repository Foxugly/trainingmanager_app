package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onChangePassword: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    if (viewModel.isLoading) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }
    viewModel.loadError?.let { err ->
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(err)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text(ProfileStrings.back) }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text(ProfileStrings.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        viewModel.saveError?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {},
            label = { Text(ProfileStrings.emailLabel) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.firstName,
            onValueChange = { viewModel.firstName = it },
            label = { Text(ProfileStrings.firstName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.lastName,
            onValueChange = { viewModel.lastName = it },
            label = { Text(ProfileStrings.lastName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text(ProfileStrings.language, style = MaterialTheme.typography.labelLarge)
        var expanded by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ProfileStrings.languageNames[viewModel.language] ?: viewModel.language)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            viewModel.languages.forEach { code ->
                DropdownMenuItem(
                    text = { Text(ProfileStrings.languageNames[code] ?: code) },
                    onClick = { viewModel.language = code; expanded = false },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(ProfileStrings.weeklyRecap, modifier = Modifier.weight(1f))
            Switch(checked = viewModel.weeklyRecapOptIn, onCheckedChange = { viewModel.weeklyRecapOptIn = it })
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(ProfileStrings.digestEmail, modifier = Modifier.weight(1f))
            Switch(checked = viewModel.digestEmail, onCheckedChange = { viewModel.digestEmail = it })
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { scope.launch { viewModel.save() } },
            enabled = !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(ProfileStrings.save)
            }
        }
        if (viewModel.saved) {
            Spacer(Modifier.height(8.dp))
            Text(ProfileStrings.saved, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Text(ProfileStrings.changePasswordCta)
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(ProfileStrings.back)
        }
    }
}
