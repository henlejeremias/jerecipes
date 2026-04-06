@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jerecipes.data.AppSettings
import com.jerecipes.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal enum class GeminiSheetCellPosition { Top, Middle, Bottom, Solo }

internal fun geminiSheetCellShape(position: GeminiSheetCellPosition): RoundedCornerShape {
    val outer = 28.dp
    val inner = 4.dp
    return when (position) {
        GeminiSheetCellPosition.Solo -> RoundedCornerShape(outer)
        GeminiSheetCellPosition.Top -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        GeminiSheetCellPosition.Middle -> RoundedCornerShape(inner)
        GeminiSheetCellPosition.Bottom -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
    }
}

@Composable
internal fun GeminiSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
internal fun GeminiSettingsCell(
    title: String,
    subtitle: String? = null,
    position: GeminiSheetCellPosition = GeminiSheetCellPosition.Solo,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = geminiSheetCellShape(position),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
            },
            supportingContent = if (subtitle != null) {
                { Text(text = subtitle, style = MaterialTheme.typography.bodyMedium) }
            } else null,
            leadingContent = leading,
            trailingContent = trailing,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onSurface,
                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
internal fun GeminiSettingsAvatar(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
internal fun GeminiPreferenceSheetScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
internal fun GeminiPreferenceSheetActions(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
internal fun geminiPreferenceSheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
internal fun GeminiCustomInstructionsSheetContent(
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    coroutineScope: CoroutineScope,
    onFinished: () -> Unit,
) {
    var customPromptDraft by remember(settings.customPrompt) { mutableStateOf(settings.customPrompt) }
    val hasPromptChanges = customPromptDraft != settings.customPrompt
    val hasSavedPrompt = settings.customPrompt.isNotBlank()

    GeminiPreferenceSheetScaffold(title = "Custom Instructions") {
        OutlinedTextField(
            value = customPromptDraft,
            onValueChange = { customPromptDraft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add instructions") },
            minLines = 5,
            maxLines = 10,
            shape = MaterialTheme.shapes.large,
            colors = geminiPreferenceSheetTextFieldColors()
        )
        if (hasPromptChanges || hasSavedPrompt) {
            GeminiPreferenceSheetActions {
                if (hasPromptChanges) {
                    TextButton(onClick = { customPromptDraft = settings.customPrompt }) {
                        Text("Discard")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        coroutineScope.launch { settingsRepository.setCustomPrompt(customPromptDraft) }
                        onFinished()
                    }) { Text("Save") }
                } else {
                    TextButton(
                        onClick = {
                            customPromptDraft = ""
                            coroutineScope.launch { settingsRepository.setCustomPrompt("") }
                            onFinished()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
internal fun GeminiApiKeySheetContent(
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    coroutineScope: CoroutineScope,
    onFinished: () -> Unit,
) {
    var apiKeyDraft by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    val hasApiKeyChanges = apiKeyDraft != settings.customApiKey
    val hasSavedApiKey = settings.customApiKey.isNotBlank()

    GeminiPreferenceSheetScaffold(title = "Custom API Key") {
        OutlinedTextField(
            value = apiKeyDraft,
            onValueChange = { apiKeyDraft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("AIza...") },
            visualTransformation = if (apiKeyVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    Text(if (apiKeyVisible) "Hide" else "Show")
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = geminiPreferenceSheetTextFieldColors()
        )
        if (hasApiKeyChanges || hasSavedApiKey) {
            GeminiPreferenceSheetActions {
                if (hasApiKeyChanges) {
                    TextButton(onClick = { apiKeyDraft = settings.customApiKey }) {
                        Text("Discard")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        coroutineScope.launch { settingsRepository.setCustomApiKey(apiKeyDraft) }
                        onFinished()
                    }) { Text("Save") }
                } else {
                    TextButton(
                        onClick = {
                            apiKeyDraft = ""
                            coroutineScope.launch { settingsRepository.setCustomApiKey("") }
                            onFinished()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Remove") }
                }
            }
        }
    }
}
