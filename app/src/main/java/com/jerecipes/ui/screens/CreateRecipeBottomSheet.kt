@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jerecipes.data.GeminiService
import com.jerecipes.data.model.Recipe
import com.jerecipes.ui.RecipeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateRecipeBottomSheet(
    onDismissRequest: () -> Unit,
    onSubmit: suspend (Recipe, Bitmap?) -> Unit,
    onBlankRecipe: () -> Unit,
    recipeViewModel: RecipeViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val geminiService: () -> GeminiService = { recipeViewModel.currentGeminiService() }

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

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
        onDismissRequest = { if (!isProcessing) onDismissRequest() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = {
            if (!isProcessing) {
                Surface(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape
                ) {
                    Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .animateContentSize()
        ) {
            if (isProcessing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    var subtext by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        val messages = listOf("Hold on...", "Almost there...", "Refining flavors...")
                        var i = 0
                        while(true) {
                            delay(3500)
                            subtext = messages[i % messages.size]
                            i++
                        }
                    }
                    
                    AnimatedVisibility(visible = subtext.isNotEmpty()) {
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                Column {
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

                    if (selectedBitmap != null) {
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
                                scope.launch {
                                    val clip = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        inputText = clip
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
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

                    Button(
                        onClick = {
                            isProcessing = true
                            statusMessage = "Summoning Gemini to parse your recipe..."
                            scope.launch {
                                try {
                                    val parsingResult = geminiService().parseRecipe(inputText, selectedBitmap)
                                    if (parsingResult.isSuccess) {
                                        var parsedRecipe = parsingResult.getOrNull()
                                        if (parsedRecipe != null) {
                                            var finalBitmap = selectedBitmap

                                            // If user hasn't provided an image, generate one with AI
                                            if (finalBitmap == null) {
                                                statusMessage = "Generating image..."
                                                val imageResult = try {
                                                    geminiService().generateImage(parsedRecipe)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("CreateRecipe", "Image generation crashed", e)
                                                    Result.failure(e)
                                                }
                                                
                                                if (imageResult.isSuccess) {
                                                    finalBitmap = imageResult.getOrNull()
                                                } else {
                                                    android.util.Log.w("CreateRecipe", "Image generation failed: ${imageResult.exceptionOrNull()?.message}")
                                                }
                                            }

                                            statusMessage = "Saving..."
                                            onSubmit(parsedRecipe, finalBitmap)
                                        }
                                    } else {
                                        isProcessing = false
                                        val errorDetail = parsingResult.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "Failed to parse: $errorDetail", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    isProcessing = false
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
