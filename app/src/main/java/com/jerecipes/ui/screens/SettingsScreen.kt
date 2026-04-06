@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jerecipes.data.GeminiService
import com.jerecipes.ui.RecipeViewModel
import kotlinx.coroutines.launch

private enum class SettingsSheet {
    SystemPrompt, CustomPrompt, ApiKey
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

    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }

    val hasSavedPrompt = settings.customPrompt.isNotBlank()
    val hasSavedApiKey = settings.customApiKey.isNotBlank()

    if (activeSheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 0.dp,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            BackHandler {
                scope.launch {
                    sheetState.hide()
                    activeSheet = null
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                when (activeSheet) {
                    SettingsSheet.SystemPrompt -> {
                        GeminiPreferenceSheetScaffold(title = "Built-in prompt") {
                            Text(
                                text = baseSystemPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsSheet.CustomPrompt -> {
                        GeminiCustomInstructionsSheetContent(
                            settings = settings,
                            settingsRepository = settingsRepo,
                            coroutineScope = scope,
                            onFinished = { activeSheet = null }
                        )
                    }

                    SettingsSheet.ApiKey -> {
                        GeminiApiKeySheetContent(
                            settings = settings,
                            settingsRepository = settingsRepo,
                            coroutineScope = scope,
                            onFinished = { activeSheet = null }
                        )
                    }

                    null -> Unit
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                GeminiSettingsGroup {
                    GeminiSettingsCell(
                        title = "Custom API Key",
                        subtitle = if (hasSavedApiKey) "Added" else "Not set",
                        position = GeminiSheetCellPosition.Solo,
                        onClick = { activeSheet = SettingsSheet.ApiKey }
                    )
                }
            }

            item {
                SectionLabel("Instructions")
            }

            item {
                GeminiSettingsGroup {
                    GeminiSettingsCell(
                        title = "Built-in prompt",
                        position = GeminiSheetCellPosition.Top,
                        onClick = { activeSheet = SettingsSheet.SystemPrompt }
                    )
                    GeminiSettingsCell(
                        title = "Custom Instructions",
                        subtitle = if (hasSavedPrompt) "On" else "Off",
                        position = GeminiSheetCellPosition.Bottom,
                        onClick = { activeSheet = SettingsSheet.CustomPrompt }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
    )
}
