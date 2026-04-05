@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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

    if (isProcessing) {
        GeminiBottomSheetShell(
            onDismissRequest = onDismissRequest,
            dismissEnabled = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(64.dp),
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
                    while (true) {
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
        }
    } else {
        GeminiPromptBottomSheet(
            onDismissRequest = onDismissRequest,
            dismissEnabled = true,
            autoFocusPrompt = true,
            prompt = inputText,
            onPromptChange = { inputText = it },
            onSend = {
                isProcessing = true
                statusMessage = "Summoning Gemini to parse your recipe..."
                scope.launch {
                    try {
                        val parsingResult = geminiService().parseRecipe(inputText, selectedBitmap)
                        if (parsingResult.isSuccess) {
                            val parsedRecipe = parsingResult.getOrNull()
                            if (parsedRecipe != null) {
                                statusMessage = "Generating AI image..."
                                val imageResult = try {
                                    geminiService().generateImage(parsedRecipe)
                                } catch (e: Exception) {
                                    android.util.Log.e("CreateRecipe", "Image generation crashed", e)
                                    Result.failure(e)
                                }

                                if (imageResult.isFailure) {
                                    isProcessing = false
                                    val errorDetail = imageResult.exceptionOrNull()?.message ?: "Unknown error"
                                    Toast.makeText(
                                        context,
                                        "Failed to generate AI image: $errorDetail",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }

                                val generatedBitmap = imageResult.getOrNull()
                                if (generatedBitmap == null) {
                                    isProcessing = false
                                    Toast.makeText(context, "AI image generation returned no image", Toast.LENGTH_LONG).show()
                                    return@launch
                                }

                                statusMessage = "Saving..."
                                onSubmit(parsedRecipe, generatedBitmap)
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
            placeholder = "Just type if you're fancy!",
            sendEnabled = (inputText.isNotEmpty() || selectedBitmap != null),
            supportingContent = {
                if (selectedBitmap != null) {
                    Box(
                        modifier = Modifier
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
            },
            leadingActions = {
                GeminiPromptSheetActionIcon(
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
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = "Clipboard",
                        modifier = Modifier.size(22.dp)
                    )
                }

                GeminiPromptSheetActionIcon(
                    onClick = {
                        onDismissRequest()
                        onBlankRecipe()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Blank recipe",
                        modifier = Modifier.size(22.dp)
                    )
                }

                GeminiPromptSheetActionIcon(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Add image",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }
}
