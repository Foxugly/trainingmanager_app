package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.foxugly.trainingmanager_app.i18n.LocalStrings

/**
 * Single password field used across login / register / change-password, with a
 * built-in show/hide toggle. Visibility is local UI state (each field manages
 * its own), and the toggle's accessibility label is pulled from [LocalStrings]
 * so callers only pass the field label + value.
 */
@Composable
fun PasswordField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    val s = LocalStrings.current
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) PasswordHiddenIcon else PasswordVisibleIcon,
                    contentDescription = if (visible) s.hidePassword else s.showPassword,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        modifier = modifier.fillMaxWidth(),
    )
}
