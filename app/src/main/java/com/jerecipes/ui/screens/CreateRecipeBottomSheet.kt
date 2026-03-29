@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.jerecipes.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.data.GeminiService
import com.jerecipes.data.model.Recipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateRecipeBottomSheet(
    onDismissRequest: () -> Unit,
    onRecipeParsed: (Recipe, Bitmap?) -> Unit,
    onBlankRecipe: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val geminiService = remember { GeminiService() }

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isParsing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {

            // ── Gemini-style Text Input ──────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    placeholder = {
                        Text(
                            "Just type if you're fancy!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(28.dp),
                    minLines = 2,
                    maxLines = 5
                )
            }

            AnimatedVisibility(
                visible = selectedBitmap != null,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    selectedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    FilledIconButton(
                        onClick = { selectedBitmap = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove image",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Option Cards ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ContentPaste,
                    label = "Clipboard",
                    description = "Paste copied text",
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            inputText = clip
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                OptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.EditNote,
                    label = "Blank",
                    description = "Start from scratch",
                    onClick = {
                        onDismissRequest()
                        onBlankRecipe()
                    }
                )

                OptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.AddPhotoAlternate,
                    label = "Image",
                    description = "Upload a photo",
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Bottom Action Section: Swap Button vs Progress ─────────────
            AnimatedContent(
                targetState = isParsing,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                     scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                        scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                    )
                },
                label = "parsingTransition"
            ) { parsing ->
                if (parsing) {
                    // Loading indicator with Native Wavy Progress
                    var progressTextIndex by remember { mutableIntStateOf(0) }
                    val progressTexts = listOf("Working for you...", "Hold on...")

                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2500)
                            progressTextIndex = (progressTextIndex + 1) % progressTexts.size
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Indeterminate progress indicator
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Crossfade(targetState = progressTexts[progressTextIndex]) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Create button (Pill-Shaped / Expressive)
                    Button(
                        onClick = {
                            isParsing = true
                            scope.launch {
                                try {
                                    val result = geminiService.parseRecipe(inputText, selectedBitmap)
                                    isParsing = false
                                    if (result.isSuccess) {
                                        val parsedRecipe = result.getOrNull()
                                        if (parsedRecipe != null) {
                                            onRecipeParsed(parsedRecipe, selectedBitmap)
                                        }
                                    } else {
                                        val errorDetail = result.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "Failed to parse: $errorDetail", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    isParsing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = (inputText.isNotEmpty() || selectedBitmap != null),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Outlined.OutdoorGrill, 
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Create",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(124.dp),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
