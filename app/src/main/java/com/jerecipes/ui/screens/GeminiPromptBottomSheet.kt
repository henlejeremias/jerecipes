@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val GeminiSheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
private val GeminiSheetColor = Color(0xFFFDFCFB)

@Composable
fun GeminiBottomSheetShell(
    onDismissRequest: () -> Unit,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    val activity = view.context.findActivity()

    DisposableEffect(activity, view) {
        val window = activity?.window
        val previousNavigationBarColor = window?.navigationBarColor
        val previousLightNavigationBars = window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars
        }

        if (window != null) {
            window.navigationBarColor = GeminiSheetColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                GeminiSheetColor.luminance() > 0.5f
        }

        onDispose {
            if (window != null && previousNavigationBarColor != null && previousLightNavigationBars != null) {
                window.navigationBarColor = previousNavigationBarColor
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                    previousLightNavigationBars
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (dismissEnabled) onDismissRequest()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.10f),
        tonalElevation = 0.dp
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = GeminiSheetShape,
                    spotColor = Color.Black.copy(alpha = 0.10f),
                    ambientColor = Color.Black.copy(alpha = 0.04f)
                ),
            shape = GeminiSheetShape,
            color = GeminiSheetColor
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                content()
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

@Composable
fun GeminiPromptBottomSheet(
    onDismissRequest: () -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    autoFocusPrompt: Boolean = false,
    sendEnabled: Boolean = true,
    promptMinHeight: Dp = 88.dp,
    maxPromptLines: Int = 6,
    horizontalPadding: Dp = 30.dp,
    topPadding: Dp = 26.dp,
    bottomContentPadding: Dp = 18.dp,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    leadingActions: @Composable RowScope.() -> Unit = {}
) {
    val promptStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
    )

    GeminiBottomSheetShell(
        onDismissRequest = onDismissRequest,
        dismissEnabled = dismissEnabled
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = topPadding,
                    bottom = bottomContentPadding
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = promptMinHeight),
                verticalArrangement = Arrangement.spacedBy(
                    if (prompt.isBlank()) 0.dp else 18.dp
                )
            ) {
                BasicTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = promptMinHeight),
                    textStyle = promptStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = maxPromptLines,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (prompt.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = promptStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                supportingContent()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingActions()

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onSend,
                    enabled = sendEnabled,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GeminiPromptSheetActionIcon(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
        )
    ) {
        icon()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
