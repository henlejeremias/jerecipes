@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
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

private enum class LibraryPreferencesLayer {
    Menu,
    ApiKey,
    BuiltInPrompt,
    CustomInstructions,
}

@Composable
fun LibraryPreferencesBottomSheet(
    recipeViewModel: RecipeViewModel,
    onDismissRequest: () -> Unit,
) {
    val settings by recipeViewModel.settings.collectAsState()
    val settingsRepo = recipeViewModel.settingsRepository
    val scope = rememberCoroutineScope()
    var layer by remember { mutableStateOf(LibraryPreferencesLayer.Menu) }

    val baseSystemPrompt = remember { GeminiService().systemPrompt }

    val hasSavedApiKey = settings.customApiKey.isNotBlank()
    val hasSavedPrompt = settings.customPrompt.isNotBlank()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        BackHandler {
            when (layer) {
                LibraryPreferencesLayer.Menu -> {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                }
                else -> layer = LibraryPreferencesLayer.Menu
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
            when (layer) {
                LibraryPreferencesLayer.Menu -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GeminiSettingsGroup {
                            GeminiSettingsCell(
                                title = "Custom API Key",
                                subtitle = if (hasSavedApiKey) "Added" else "Not set",
                                position = GeminiSheetCellPosition.Top,
                                onClick = { layer = LibraryPreferencesLayer.ApiKey }
                            )
                            GeminiSettingsCell(
                                title = "Built-in prompt",
                                position = GeminiSheetCellPosition.Middle,
                                onClick = { layer = LibraryPreferencesLayer.BuiltInPrompt }
                            )
                            GeminiSettingsCell(
                                title = "Custom Instructions",
                                subtitle = if (hasSavedPrompt) "On" else "Off",
                                position = GeminiSheetCellPosition.Bottom,
                                onClick = { layer = LibraryPreferencesLayer.CustomInstructions }
                            )
                        }
                    }
                }

                LibraryPreferencesLayer.ApiKey -> {
                    GeminiApiKeySheetContent(
                        settings = settings,
                        settingsRepository = settingsRepo,
                        coroutineScope = scope,
                        onFinished = { layer = LibraryPreferencesLayer.Menu }
                    )
                }

                LibraryPreferencesLayer.BuiltInPrompt -> {
                    GeminiPreferenceSheetScaffold(title = "Built-in prompt") {
                        Text(
                            text = baseSystemPrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LibraryPreferencesLayer.CustomInstructions -> {
                    GeminiCustomInstructionsSheetContent(
                        settings = settings,
                        settingsRepository = settingsRepo,
                        coroutineScope = scope,
                        onFinished = { layer = LibraryPreferencesLayer.Menu }
                    )
                }
            }
        }
    }
}
