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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jerecipes.R
import com.jerecipes.data.GeminiService
import com.jerecipes.data.model.Recipe
import com.jerecipes.ui.RecipeViewModel
import kotlinx.coroutines.launch

@Composable
fun CreateRecipeBottomSheet(
    onDismissRequest: () -> Unit,
    onSubmit: suspend (Recipe, Bitmap?) -> Unit,
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

    GeminiBottomSheetShell(
        onDismissRequest = onDismissRequest,
        dismissEnabled = !isProcessing
    ) {
        if (isProcessing) {
            GeminiSheetLoadingColumn(
                statusLine = statusMessage,
                rotatingHints = listOf("Hold on...", "Almost there...", "Still working..."),
                hintIntervalMillis = 3500L,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            GeminiPromptBottomSheetBody(
                    prompt = inputText,
                    onPromptChange = { inputText = it },
                    onSend = {
                        isProcessing = true
                        statusMessage = "Parsing recipe..."
                        scope.launch {
                            try {
                                val parsingResult = geminiService().parseRecipe(inputText, selectedBitmap)
                                if (parsingResult.isSuccess) {
                                    val parsedRecipe = parsingResult.getOrNull()
                                    if (parsedRecipe != null) {
                                        statusMessage = "Generating image..."
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
                                            Toast.makeText(
                                                context,
                                                "AI image generation returned no image",
                                                Toast.LENGTH_LONG
                                            ).show()
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
                        modifier = Modifier.size(20.dp)
                    )
                }

                GeminiPromptSheetActionIcon(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.convert_to_text_24),
                        contentDescription = "Add image",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            )
        }
    }
}
