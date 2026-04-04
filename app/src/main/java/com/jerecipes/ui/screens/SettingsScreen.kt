@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.jerecipes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jerecipes.data.GeminiModel
import com.jerecipes.data.GeminiService
import com.jerecipes.ui.RecipeViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    recipeViewModel: RecipeViewModel,
    onBack: () -> Unit
) {
    val settings by recipeViewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val settingsRepo = recipeViewModel.settingsRepository

    val baseSystemPrompt = remember { GeminiService().systemPrompt }
    val selectedModel = GeminiModel.entries.firstOrNull { it.id == settings.modelName } ?: GeminiModel.FLASH_LITE

    var customPromptDraft by remember(settings.customPrompt) { mutableStateOf(settings.customPrompt) }
    var apiKeyDraft by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showSystemPrompt by remember { mutableStateOf(false) }

    val hasPromptChanges = customPromptDraft != settings.customPrompt
    val hasSavedPrompt = settings.customPrompt.isNotBlank()
    val hasApiKeyChanges = apiKeyDraft != settings.customApiKey
    val hasSavedApiKey = settings.customApiKey.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 40.dp
            )
        ) {
            item {
                SettingsHeroCard()
            }

            item {
                SettingsSectionHeader(
                    eyebrow = "Intelligence",
                    title = "Choose how Jerecipes thinks",
                    subtitle = "Switch between faster everyday replies and a more capable model for deeper recipe work."
                )
            }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SettingsCardHeader(
                            icon = Icons.Outlined.Psychology,
                            title = "Preferred Gemini model",
                            subtitle = "Your selection is saved instantly and used across recipe generation and editing."
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            GeminiModel.entries.forEachIndexed { index, model ->
                                val isSelected = settings.modelName == model.id
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { scope.launch { settingsRepo.setModel(model.id) } },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 76.dp)
                                        .semantics { role = Role.RadioButton },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        GeminiModel.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = model.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (isSelected) "Selected" else "Tap to switch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SettingsIconBadge(
                                    icon = if (selectedModel == GeminiModel.FLASH_LITE) Icons.Outlined.Bolt else Icons.Outlined.AutoAwesome,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedModel.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = modelDescription(selectedModel),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader(
                    eyebrow = "Core Instructions",
                    title = "Shape the assistant's voice",
                    subtitle = "Review the built-in prompt and add your own household rules, dietary preferences, or formatting defaults."
                )
            }

            item {
                SettingsCard(
                    modifier = Modifier.animateContentSize()
                ) {
                    Column {
                        SettingsDisclosureRow(
                            title = "Built-in system prompt",
                            subtitle = if (showSystemPrompt) {
                                "Tap to collapse the default instructions."
                            } else {
                                "Review the base guidance Jerecipes always sends to Gemini."
                            },
                            icon = Icons.Outlined.Code,
                            onClick = { showSystemPrompt = !showSystemPrompt },
                            trailing = {
                                Icon(
                                    imageVector = if (showSystemPrompt) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        AnimatedVisibility(
                            visible = showSystemPrompt,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            ) {
                                Text(
                                    text = baseSystemPrompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsEditorCard(
                            icon = Icons.Outlined.Tune,
                            title = "Custom instructions",
                            subtitle = "Add preferences like metric units, pantry assumptions, dietary boundaries, or tone."
                        ) {
                            OutlinedTextField(
                                value = customPromptDraft,
                                onValueChange = { customPromptDraft = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("e.g. Prefer unit measurements in grams.")
                                },
                                shape = MaterialTheme.shapes.large,
                                minLines = 4,
                                maxLines = 10,
                                colors = expressiveTextFieldColors()
                            )

                            Text(
                                text = "Saved instructions are added on top of the built-in prompt for every AI request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (hasPromptChanges || hasSavedPrompt) {
                                SettingsActionRow {
                                    if (hasPromptChanges) {
                                        TextButton(onClick = { customPromptDraft = settings.customPrompt }) {
                                            Text("Discard")
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = { scope.launch { settingsRepo.setCustomPrompt(customPromptDraft) } },
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text("Save")
                                        }
                                    } else {
                                        TextButton(
                                            onClick = {
                                                customPromptDraft = ""
                                                scope.launch { settingsRepo.setCustomPrompt("") }
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("Clear all")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader(
                    eyebrow = "Security & Quota",
                    title = "Bring your own API key",
                    subtitle = "Use a personal Gemini key for higher limits without changing the rest of your workflow."
                )
            }

            item {
                SettingsCard {
                    SettingsEditorCard(
                        icon = Icons.Outlined.Shield,
                        title = "Personal Gemini API key",
                        subtitle = "Stored locally on this device. Leave blank to keep using the shared app configuration."
                    ) {
                        OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("AIza...")
                            },
                            visualTransformation = if (apiKeyVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(
                                        imageVector = if (apiKeyVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (apiKeyVisible) {
                                            "Hide API key"
                                        } else {
                                            "Show API key"
                                        }
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            colors = expressiveTextFieldColors()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "A saved profile key raises your personal quota and keeps the rest of the screen unchanged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasApiKeyChanges || hasSavedApiKey) {
                            SettingsActionRow {
                                if (hasApiKeyChanges) {
                                    TextButton(onClick = { apiKeyDraft = settings.customApiKey }) {
                                        Text("Discard")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { scope.launch { settingsRepo.setCustomApiKey(apiKeyDraft) } },
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Update key")
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            apiKeyDraft = ""
                                            scope.launch { settingsRepo.setCustomApiKey("") }
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove profile key")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroCard() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                SettingsIconBadge(
                    icon = Icons.Outlined.Settings,
                    containerColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    size = 52.dp
                )
                HeaderPill(text = "Model")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Tune how Jerecipes thinks, writes, and connects to Gemini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill(text = "Instructions")
                HeaderPill(text = "API Key")
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsCardHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIconBadge(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDisclosureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsIconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SettingsEditorCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCardHeader(
            icon = icon,
            title = title,
            subtitle = subtitle
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = actions
    )
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
private fun HeaderPill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun expressiveTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
)

private fun modelDescription(model: GeminiModel): String {
    return when (model) {
        GeminiModel.FLASH_LITE -> "Faster responses for everyday recipe generation, quick edits, and short back-and-forth prompts."
        GeminiModel.FLASH -> "Better for more complex instructions, nuanced edits, and richer reasoning when the recipe needs extra care."
    }
}


