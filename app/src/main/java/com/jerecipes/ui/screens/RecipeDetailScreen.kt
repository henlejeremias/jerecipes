@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.jerecipes.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import com.jerecipes.data.model.Ingredient
import com.jerecipes.data.model.Recipe
import com.jerecipes.R
import com.jerecipes.data.model.RecipeRating
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.theme.FloatingBarBottomPadding
import com.jerecipes.ui.theme.FloatingBarCollapsedShadowElevation
import com.jerecipes.ui.theme.FloatingBarHeight
import com.jerecipes.ui.theme.FloatingBarHorizontalPadding
import com.jerecipes.ui.theme.FloatingBarShadowElevation
import com.jerecipes.ui.theme.RobotoFlexFontFamily
import com.jerecipes.ui.theme.recipeDetailTitleTextStyle
import kotlinx.coroutines.launch

internal data class RecipeFact(
    val label: String,
    val value: String,
    val unit: String,
    val icon: ImageVector
)

private enum class RecipeDetailSheet { Source, Rate }

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onRatingChange: (RecipeRating) -> Unit,
    viewModel: RecipeViewModel,
    initialScrollOffset: Int = 0
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSaving by viewModel.isSaving.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<RecipeDetailSheet?>(null) }
    var promptText by remember { mutableStateOf("") }
    var isReplacingPhoto by remember { mutableStateOf(false) }
    var deleteInProgress by remember { mutableStateOf(false) }
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

    val replacePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                isReplacingPhoto = true
                val replacementRecipe = recipe.copy(images = emptyList())
                val result = viewModel.saveRecipe(
                    recipe = replacementRecipe,
                    bitmap = bitmap,
                    imagesToDelete = recipe.images
                )
                result.exceptionOrNull()?.let { error ->
                    Toast.makeText(
                        context,
                        "Failed to update photo: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to load image: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isReplacingPhoto = false
            }
        }
    }

    LaunchedEffect(initialScrollOffset) {
        if (initialScrollOffset > 0) scrollState.scrollTo(initialScrollOffset)
    }

    LaunchedEffect(recipe.id) {
        viewModel.rememberDetailFallback(recipe)
    }
    DisposableEffect(recipe.id) {
        onDispose {
            viewModel.clearDetailFallback()
        }
    }

    // Dismiss local overlays first; register before the saving guard so the guard wins when both apply.
    BackHandler(enabled = showEditSheet || activeSheet != null) {
        when {
            showEditSheet -> {
                keyboardController?.hide()
                showEditSheet = false
                promptText = ""
            }
            activeSheet != null -> activeSheet = null
        }
    }

    // Block back while saving photo. Registered after sheet handler so this takes priority if both are active.
    BackHandler(enabled = isReplacingPhoto && isSaving) { }

    BackHandler(enabled = deleteInProgress) { }

    val doSend: () -> Unit = {
        if (promptText.isNotBlank()) {
            scope.launch {
                try {
                    val result = viewModel.editRecipeWithPrompt(recipe, promptText)
                    if (result.isSuccess) {
                        keyboardController?.hide()
                        showEditSheet = false
                        promptText = ""
                    } else {
                        Toast.makeText(
                            context,
                            "Edit failed: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(toolbarScrollBehavior)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    AsyncImage(
                        model = recipe.images.firstOrNull(),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1.0f to MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    )
                    Text(
                        text = recipe.title,
                        style = recipeDetailTitleTextStyle(
                            MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                RecipeFactsCarousel(recipe = recipe)

                if (recipe.ingredients.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recipe.ingredients.forEach { IngredientPillRow(it) }
                    }
                }

                if (recipe.instructions.isNotEmpty() || !recipe.comment.isNullOrBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recipe.instructions.forEachIndexed { index, step ->
                            InstructionCard(
                                header = "Step ${index + 1}",
                                body = step,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        recipe.comment
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { comment ->
                                InstructionCard(
                                    header = "Additional",
                                    body = comment,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = FloatingBarHorizontalPadding)
                .padding(bottom = FloatingBarBottomPadding)
                .height(FloatingBarHeight)
                .zIndex(2f),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = (FloatingBarHeight - 48.dp) / 2
            ),
            scrollBehavior = toolbarScrollBehavior,
            expandedShadowElevation = FloatingBarShadowElevation,
            collapsedShadowElevation = FloatingBarCollapsedShadowElevation
        ) {
            RecipeDetailToolbarButton(
                painter = painterResource(R.drawable.frame_source_24),
                contentDescription = "Source",
                onClick = { activeSheet = RecipeDetailSheet.Source }
            )
            RecipeDetailToolbarButton(
                painter = painterResource(R.drawable.image_arrow_up_24),
                contentDescription = "Picture",
                onClick = { replacePhotoLauncher.launch("image/*") },
                enabled = !isSaving && !isReplacingPhoto
            )
            RecipeDetailToolbarButton(
                painter = painterResource(R.drawable.edit_note_24),
                emphasized = true,
                contentDescription = "Edit",
                onClick = { showEditSheet = true },
                enabled = !isSaving && !isReplacingPhoto
            )
            RecipeDetailToolbarButton(
                painter = painterResource(R.drawable.rate_review_24),
                contentDescription = "Rate",
                onClick = { activeSheet = RecipeDetailSheet.Rate }
            )
            RecipeDetailToolbarButton(
                painter = painterResource(R.drawable.delete_forever_24),
                contentDescription = "Delete",
                onClick = {
                    deleteInProgress = true
                    viewModel.deleteRecipeFromDetail(recipe) { success, errorMessage ->
                        deleteInProgress = false
                        if (success) {
                            onBack()
                        } else {
                            Toast.makeText(
                                context,
                                "Could not delete: ${errorMessage ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                enabled = !deleteInProgress && !isSaving && !isEditing
            )
        }

        if (showEditSheet) {
            GeminiPromptBottomSheet(
                onDismissRequest = {
                    keyboardController?.hide()
                    showEditSheet = false
                    promptText = ""
                },
                dismissEnabled = !isEditing,
                isBusy = isEditing,
                busyMessage = "Applying edit...",
                autoFocusPrompt = true,
                prompt = promptText,
                onPromptChange = { promptText = it },
                onSend = doSend,
                placeholder = "Describe your change…",
                sendEnabled = promptText.isNotBlank() && !isEditing
            )
        }

        when (activeSheet) {
            RecipeDetailSheet.Source -> {
                SourceBottomSheet(
                    source = recipe.source,
                    onDismissRequest = { activeSheet = null }
                )
            }
            RecipeDetailSheet.Rate -> {
                RatingBottomSheet(
                    currentRating = recipe.parsedRating,
                    onDismissRequest = { activeSheet = null },
                    onRatingChange = onRatingChange
                )
            }
            null -> Unit
        }

        if (isReplacingPhoto && isSaving) {
            PhotoSavingBottomSheet()
        }

        if (deleteInProgress) {
            DeleteRecipeLoadingBottomSheet()
        }
    }
}

@Composable
private fun RecipeDetailToolbarButton(
    painter: Painter,
    emphasized: Boolean = false,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    if (!emphasized) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription
            )
        }
    } else {
        val containerColor = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        }
        val contentColor = if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }

        Box(
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 72.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private sealed class SourceSheetContent {
    data object None : SourceSheetContent()
    data class Link(val displayText: String, val openUri: String) : SourceSheetContent()
    data class Plain(val text: String) : SourceSheetContent()
}

private fun sourceSheetContent(source: String?): SourceSheetContent {
    val raw = source?.trim().orEmpty()
    if (raw.isEmpty()) return SourceSheetContent.None
    val withScheme = when {
        raw.startsWith("http://", ignoreCase = true) -> raw
        raw.startsWith("https://", ignoreCase = true) -> raw
        else -> "https://$raw"
    }
    val looksLikeUrl = Patterns.WEB_URL.matcher(raw).matches() ||
        Patterns.WEB_URL.matcher(withScheme).matches()
    return if (looksLikeUrl) SourceSheetContent.Link(displayText = raw, openUri = withScheme)
    else SourceSheetContent.Plain(raw)
}

@Composable
private fun SourceBottomSheet(
    source: String?,
    onDismissRequest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val content = remember(source) { sourceSheetContent(source) }
    GeminiBottomSheetShell(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Source",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (content) {
                SourceSheetContent.None -> Unit
                is SourceSheetContent.Link -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri(content.openUri) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = content.displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is SourceSheetContent.Plain -> {
                    Text(
                        text = content.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingBottomSheet(
    currentRating: RecipeRating,
    onDismissRequest: () -> Unit,
    onRatingChange: (RecipeRating) -> Unit
) {
    GeminiBottomSheetShell(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Rate",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            RatingButtonGroup(
                currentRating = currentRating,
                onRatingChange = onRatingChange
            )
        }
    }
}

@Composable
private fun PhotoSavingBottomSheet() {
    GeminiBottomSheetShell(
        onDismissRequest = {},
        dismissEnabled = false
    ) {
        GeminiSheetLoadingColumn(
            statusLine = "Saving...",
            rotatingHints = listOf(
                "Almost there...",
                "Still working..."
            ),
            hintIntervalMillis = 2400L,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeleteRecipeLoadingBottomSheet() {
    GeminiBottomSheetShell(
        onDismissRequest = {},
        dismissEnabled = false
    ) {
        GeminiSheetLoadingColumn(
            statusLine = "Deleting recipe…",
            rotatingHints = listOf(
                "Almost there...",
                "Still working..."
            ),
            hintIntervalMillis = 2400L,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun RecipeFactsCarousel(recipe: Recipe) {
    val facts = buildList {
        recipe.calories?.let { add(RecipeFact("Calories", "$it", "kcal", Icons.Outlined.LocalFireDepartment)) }
        recipe.prepTime?.let { add(RecipeFact("Prep Time", "$it", "min", Icons.Outlined.Timer)) }
        recipe.waitTime?.let { add(RecipeFact("Wait Time", "$it", "min", Icons.Outlined.HourglassBottom)) }
        recipe.protein?.let { add(RecipeFact("Protein", "$it", "g", Icons.Outlined.FitnessCenter)) }
        recipe.carbs?.let { add(RecipeFact("Carbs", "$it", "g", Icons.Outlined.Grain)) }
        recipe.fat?.let { add(RecipeFact("Fat", "$it", "g", Icons.Outlined.WaterDrop)) }
    }

    if (facts.isEmpty()) return

    val carouselState = rememberCarouselState { facts.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(170.dp),
        preferredItemWidth = 150.dp,
        itemSpacing = 12.dp,
        flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = carouselState)
    ) { index ->
        val fact = facts[index]
        val containerColor = MaterialTheme.colorScheme.secondaryContainer
        val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .maskClip(MaterialTheme.shapes.extraLarge),
            color = containerColor,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fact.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = fact.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = fact.value,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = contentColor
                        )
                        Text(
                            text = fact.unit,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientPillRow(ingredient: Ingredient) {
    val amountStr = ingredient.amount?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: ""
    val unitStr = ingredient.unit ?: ""
    val quantityLabel = buildString {
        if (amountStr.isNotEmpty()) append(amountStr)
        if (amountStr.isNotEmpty() && unitStr.isNotEmpty()) append(" ")
        if (unitStr.isNotEmpty()) append(unitStr)
    }
    val hasQuantity = quantityLabel.isNotEmpty()
    val outerHeight = 56.dp
    val innerHeight = 36.dp

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().height(outerHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = (outerHeight - innerHeight) / 2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (hasQuantity) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.height(innerHeight)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = quantityLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(innerHeight))
            }
        }
    }
}

@Composable
private fun InstructionCard(
    header: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = RobotoFlexFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 34.sp
                ),
                color = contentColor
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun RatingButtonGroup(
    currentRating: RecipeRating,
    onRatingChange: (RecipeRating) -> Unit,
    modifier: Modifier = Modifier
) {
    data class RatingOption(val rating: RecipeRating, val label: String, val icon: ImageVector?)

    val options = listOf(
        RatingOption(RecipeRating.TOP,  "Top",  Icons.Outlined.Favorite),
        RatingOption(RecipeRating.GOOD, "Good", null),
        RatingOption(RecipeRating.MID,  "Mid",  null),
        RatingOption(RecipeRating.NEW,  "New",  null),
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, opt ->
            val isSelected = currentRating == opt.rating
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onRatingChange(opt.rating) },
                modifier = Modifier
                    .weight(if (isSelected) 1.5f else 1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0                 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else              -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    opt.icon?.let { imageVector ->
                        Icon(
                            imageVector = imageVector,
                            contentDescription = opt.label,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    if (opt.icon == null || isSelected) {
                        if (opt.icon != null && isSelected) {
                            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(
                            text = opt.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
