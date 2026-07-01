package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.foundation.layout.Box
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
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.LanguageService
import com.foxugly.trainingmanager_app.i18n.LocalStrings
import com.foxugly.trainingmanager_app.i18n.languageDisplayNames
import com.foxugly.trainingmanager_app.i18n.supportedLanguages
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.ErrorState
import com.foxugly.trainingmanager_app.ui.components.LoadingState
import com.foxugly.trainingmanager_app.ui.components.MainScaffold
import com.foxugly.trainingmanager_app.ui.components.MainTab
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    languageService: LanguageService,
    authRepository: AuthRepository,
    onSelectTab: (MainTab) -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(title = s.profileTitle, currentTab = MainTab.PROFILE, onSelectTab = onSelectTab) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.loadError != null ->
                    ErrorState(viewModel.loadError!!, onRetry = { scope.launch { viewModel.load() } }, retryLabel = s.retry)
                else -> ProfileForm(
                    viewModel = viewModel,
                    languageService = languageService,
                    onChangePassword = onChangePassword,
                    onLogout = { scope.launch { authRepository.logout(); onLoggedOut() } },
                )
            }
        }
    }
}

@Composable
private fun ProfileForm(
    viewModel: ProfileViewModel,
    languageService: LanguageService,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        viewModel.saveError?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {},
            label = { Text(s.emailLabel) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.firstName,
            onValueChange = { viewModel.firstName = it },
            label = { Text(s.firstName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.lastName,
            onValueChange = { viewModel.lastName = it },
            label = { Text(s.lastName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text(s.languageLabel, style = MaterialTheme.typography.labelLarge)
        var expanded by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(languageDisplayNames[languageService.activeLang] ?: languageService.activeLang)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { code ->
                DropdownMenuItem(
                    text = { Text(languageDisplayNames[code] ?: code) },
                    onClick = {
                        expanded = false
                        scope.launch { languageService.switchLanguage(code) }
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.weeklyRecap, modifier = Modifier.weight(1f))
            Switch(checked = viewModel.weeklyRecapOptIn, onCheckedChange = { viewModel.weeklyRecapOptIn = it })
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(s.digestEmail, modifier = Modifier.weight(1f))
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
                Text(s.save)
            }
        }
        if (viewModel.saved) {
            Spacer(Modifier.height(8.dp))
            Text(s.saved, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Text(s.changePasswordCta)
        }
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(s.logout, color = MaterialTheme.colorScheme.error)
        }
    }
}
