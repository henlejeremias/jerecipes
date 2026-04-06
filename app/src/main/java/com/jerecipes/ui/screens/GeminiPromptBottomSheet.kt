@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.jerecipes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.R
import com.jerecipes.ui.BoldCircularWavyProgressDefaultSize
import com.jerecipes.ui.BoldCircularWavyProgressIndicator
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GeminiSheetTopCornerRadius = 38.dp
private val GeminiSheetShape = RoundedCornerShape(
    topStart = GeminiSheetTopCornerRadius,
    topEnd = GeminiSheetTopCornerRadius
)
private val GeminiSheetColor = Color(0xFFFDFCFB)

/** Horizontal inset so prompt + action row sit inside the sheet’s rounded top corners. */
private val GeminiPromptCornerDeadZoneH = max(10f, GeminiSheetTopCornerRadius.value * 0.32f).dp

/** Extra top inset so the first line sits below the curved corner region. */
private val GeminiPromptCornerDeadZoneTop = max(12f, GeminiSheetTopCornerRadius.value * 0.38f).dp

/** Padding inside the text area so glyphs don’t hug the implied text box edges. */
private val GeminiPromptTextInnerPaddingH = 4.dp
private val GeminiPromptTextInnerPaddingTop = 6.dp
private val GeminiPromptTextInnerPaddingBottom = 3.dp

/** Minimum body height for loading states so sheets stay comfortably sized (not a shallow strip). */
val GeminiSheetLoadingBodyMinHeight = 268.dp

/** Circular icon targets for prompt sheet actions (outlined leading, filled send). */
private val GeminiPromptActionPillSize = 40.dp

@Composable
fun GeminiBottomSheetShell(
    onDismissRequest: () -> Unit,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            if (dismissEnabled) onDismissRequest()
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        dragHandle = null,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        // Draw sheet to the physical bottom; keep nav-bar clearance inside sheet (Spacer below).
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        BackHandler {
            if (!dismissEnabled) return@BackHandler
            scope.launch {
                sheetState.hide()
                onDismissRequest()
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                verticalArrangement = Arrangement.Top
            ) {
                content()
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

/** Compact loading body for Gemini-style sheets (create flow, photo replace, busy prompt, etc.). */
@Composable
fun GeminiSheetLoadingColumn(
    statusLine: String,
    rotatingHints: List<String>,
    hintIntervalMillis: Long = 2800L,
    modifier: Modifier = Modifier,
    progressSize: Dp = BoldCircularWavyProgressDefaultSize
) {
    val reserveHintLine = rotatingHints.isNotEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GeminiSheetLoadingBodyMinHeight)
            .wrapContentHeight()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoldCircularWavyProgressIndicator(modifier = Modifier.size(progressSize))
        Spacer(Modifier.size(16.dp))
        Text(
            text = statusLine,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        var subtext by remember { mutableStateOf("") }
        LaunchedEffect(rotatingHints, hintIntervalMillis) {
            if (rotatingHints.isEmpty()) return@LaunchedEffect
            var i = 0
            while (true) {
                delay(hintIntervalMillis)
                subtext = rotatingHints[i % rotatingHints.size]
                i++
            }
        }
        if (reserveHintLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(visible = subtext.isNotEmpty()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Prompt field + actions only (no [GeminiBottomSheetShell]). Use inside a single sheet shell so
 * switching to/from loading animates height smoothly.
 */
@Composable
fun GeminiPromptBottomSheetBody(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    sendEnabled: Boolean = true,
    promptMinHeight: Dp = 72.dp,
    maxPromptLines: Int = 6,
    horizontalPadding: Dp = 22.dp,
    topPadding: Dp = 18.dp,
    bottomContentPadding: Dp = 22.dp,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    leadingActions: @Composable RowScope.() -> Unit = {}
) {
    val promptStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomContentPadding
            )
            .padding(
                start = GeminiPromptCornerDeadZoneH,
                end = GeminiPromptCornerDeadZoneH,
                top = GeminiPromptCornerDeadZoneTop
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = promptMinHeight),
            verticalArrangement = Arrangement.spacedBy(
                if (prompt.isBlank()) 0.dp else 10.dp
            )
        ) {
            BasicTextField(
                value = prompt,
                onValueChange = onPromptChange,
                readOnly = readOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = promptMinHeight),
                textStyle = promptStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = maxPromptLines,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = GeminiPromptTextInnerPaddingH,
                                end = GeminiPromptTextInnerPaddingH,
                                top = GeminiPromptTextInnerPaddingTop,
                                bottom = GeminiPromptTextInnerPaddingBottom
                            ),
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

        // Same lateral guide as the BasicTextField (full width of this column); text stays inset via decorationBox only.
        val canSend = sendEnabled && !readOnly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leadingActions()

            Spacer(modifier = Modifier.weight(1f))

            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(GeminiPromptActionPillSize),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.terminal_24),
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
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
    isBusy: Boolean = false,
    busyMessage: String = "Applying edit...",
    autoFocusPrompt: Boolean = false,
    sendEnabled: Boolean = true,
    promptMinHeight: Dp = 72.dp,
    maxPromptLines: Int = 6,
    horizontalPadding: Dp = 22.dp,
    topPadding: Dp = 18.dp,
    bottomContentPadding: Dp = 22.dp,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    leadingActions: @Composable RowScope.() -> Unit = {}
) {
    GeminiBottomSheetShell(
        onDismissRequest = onDismissRequest,
        dismissEnabled = dismissEnabled && !isBusy
    ) {
        if (isBusy) {
            GeminiSheetLoadingColumn(
                statusLine = busyMessage,
                rotatingHints = emptyList(),
                modifier = modifier.fillMaxWidth()
            )
        } else {
            GeminiPromptBottomSheetBody(
                prompt = prompt,
                onPromptChange = onPromptChange,
                onSend = onSend,
                placeholder = placeholder,
                readOnly = false,
                sendEnabled = sendEnabled,
                promptMinHeight = promptMinHeight,
                maxPromptLines = maxPromptLines,
                horizontalPadding = horizontalPadding,
                topPadding = topPadding,
                bottomContentPadding = bottomContentPadding,
                modifier = modifier.fillMaxWidth(),
                supportingContent = supportingContent,
                leadingActions = leadingActions
            )
        }
    }
}

@Composable
fun GeminiPromptSheetActionIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val borderAlpha = if (enabled) 0.48f else 0.22f
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = if (enabled) 0.88f else 0.35f
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
        ),
        modifier = Modifier.size(GeminiPromptActionPillSize)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}
