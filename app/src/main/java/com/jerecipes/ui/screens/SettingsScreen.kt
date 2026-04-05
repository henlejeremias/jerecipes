@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jerecipes.data.GeminiModel
import com.jerecipes.data.GeminiService
import com.jerecipes.ui.RecipeViewModel
import kotlinx.coroutines.launch

private enum class CellPosition { Top, Middle, Bottom, Solo }

private fun cellShape(position: CellPosition): RoundedCornerShape {
    val outer = 28.dp
    val inner = 4.dp
    return when (position) {
        CellPosition.Solo   -> RoundedCornerShape(outer)
        CellPosition.Top    -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        CellPosition.Middle -> RoundedCornerShape(inner)
        CellPosition.Bottom -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
    }
}

private enum class SettingsSheet {
    Model, SystemPrompt, CustomPrompt, ApiKey
}

@Composable
fun SettingsScreen(
    recipeViewModel: RecipeViewModel,
    onBack: () -> Unit
) {
    val settings by recipeViewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val settingsRepo = recipeViewModel.settingsRepository

    val baseSystemPrompt = remember { GeminiService().systemPrompt }
    val selectedModel = GeminiModel.entries.firstOrNull { it.id == settings.modelName }
        ?: GeminiModel.FLASH_LITE

    var customPromptDraft by remember(settings.customPrompt) { mutableStateOf(settings.customPrompt) }
    var apiKeyDraft by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }

    val hasPromptChanges = customPromptDraft != settings.customPrompt
    val hasSavedPrompt = settings.customPrompt.isNotBlank()
    val hasApiKeyChanges = apiKeyDraft != settings.customApiKey
    val hasSavedApiKey = settings.customApiKey.isNotBlank()

    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 0.dp
        ) {
            when (activeSheet) {
                SettingsSheet.Model -> {
                    SheetScaffold(title = "Model") {
                        SettingsGroup {
                            GeminiModel.entries.forEachIndexed { index, model ->
                                val position = when {
                                    GeminiModel.entries.size == 1 -> CellPosition.Solo
                                    index == 0 -> CellPosition.Top
                                    index == GeminiModel.entries.lastIndex -> CellPosition.Bottom
                                    else -> CellPosition.Middle
                                }
                                SettingsCell(
                                    title = model.displayName,
                                    subtitle = if (model == GeminiModel.FLASH_LITE) "Faster" else "More capable",
                                    position = position,
                                    trailing = if (settings.modelName == model.id) {
                                        { Text("On", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        scope.launch { settingsRepo.setModel(model.id) }
                                        activeSheet = null
                                    }
                                )
                            }
                        }
                    }
                }

                SettingsSheet.SystemPrompt -> {
                    SheetScaffold(title = "Built-in prompt") {
                        Text(
                            text = baseSystemPrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsSheet.CustomPrompt -> {
                    SheetScaffold(title = "Custom instructions") {
                        OutlinedTextField(
                            value = customPromptDraft,
                            onValueChange = { customPromptDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add instructions") },
                            minLines = 5,
                            maxLines = 10,
                            shape = MaterialTheme.shapes.large,
                            colors = sheetTextFieldColors()
                        )
                        if (hasPromptChanges || hasSavedPrompt) {
                            SheetActions {
                                if (hasPromptChanges) {
                                    TextButton(onClick = { customPromptDraft = settings.customPrompt }) {
                                        Text("Discard")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        scope.launch { settingsRepo.setCustomPrompt(customPromptDraft) }
                                        activeSheet = null
                                    }) { Text("Save") }
                                } else {
                                    TextButton(
                                        onClick = {
                                            customPromptDraft = ""
                                            scope.launch { settingsRepo.setCustomPrompt("") }
                                            activeSheet = null
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

                SettingsSheet.ApiKey -> {
                    SheetScaffold(title = "API key") {
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
                            colors = sheetTextFieldColors()
                        )
                        if (hasApiKeyChanges || hasSavedApiKey) {
                            SheetActions {
                                if (hasApiKeyChanges) {
                                    TextButton(onClick = { apiKeyDraft = settings.customApiKey }) {
                                        Text("Discard")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        scope.launch { settingsRepo.setCustomApiKey(apiKeyDraft) }
                                        activeSheet = null
                                    }) { Text("Save") }
                                } else {
                                    TextButton(
                                        onClick = {
                                            apiKeyDraft = ""
                                            scope.launch { settingsRepo.setCustomApiKey("") }
                                            activeSheet = null
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

                null -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 40.dp
            )
        ) {
            item {
                SettingsGroup {
                    SettingsCell(
                        title = "Gemini",
                        subtitle = selectedModel.displayName,
                        position = CellPosition.Top,
                        onClick = { activeSheet = SettingsSheet.Model },
                        leading = { SettingsAvatar("AI") }
                    )
                    SettingsCell(
                        title = "API key",
                        subtitle = if (hasSavedApiKey) "Added" else "Not set",
                        position = CellPosition.Bottom,
                        onClick = { activeSheet = SettingsSheet.ApiKey },
                        leading = { SettingsAvatar("K") }
                    )
                }
            }

            item {
                SectionLabel("Instructions")
            }

            item {
                SettingsGroup {
                    SettingsCell(
                        title = "Built-in prompt",
                        position = CellPosition.Top,
                        onClick = { activeSheet = SettingsSheet.SystemPrompt }
                    )
                    SettingsCell(
                        title = "Custom instructions",
                        subtitle = if (hasSavedPrompt) "On" else "Off",
                        position = CellPosition.Bottom,
                        onClick = { activeSheet = SettingsSheet.CustomPrompt }
                    )
                }
            }
        }
    }
}

// ── Shared list primitives ──────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsGroup(
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
private fun SettingsCell(
    title: String,
    subtitle: String? = null,
    position: CellPosition = CellPosition.Solo,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = cellShape(position),
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
private fun SettingsAvatar(text: String) {
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

// ── Sheet scaffolding ────────────────────────────────────────────────────────

@Composable
private fun SheetScaffold(
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
private fun SheetActions(
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
private fun sheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface
)
