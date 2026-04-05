package com.jerecipes.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun GeminiPromptBottomSheetPrototype(
    onDismissRequest: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    GeminiPromptBottomSheet(
        onDismissRequest = onDismissRequest,
        prompt = prompt,
        onPromptChange = { prompt = it },
        onSend = {},
        placeholder = "Ask Gemini",
        leadingActions = {
            GeminiPromptSheetActionIcon(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add",
                    modifier = androidx.compose.ui.Modifier.size(30.dp)
                )
            }
            GeminiPromptSheetActionIcon(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = "Clipboard",
                    modifier = androidx.compose.ui.Modifier.size(22.dp)
                )
            }
        }
    )
}
