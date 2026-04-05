@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.jerecipes.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil.compose.AsyncImage
import com.jerecipes.data.model.Ingredient
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.theme.recipeDetailTitleTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class RecipeFact(
    val label: String,
    val value: String,
    val unit: String,
    val icon: ImageVector
)

private enum class EditBarState { Fab, Expanding, Input, Loading, Collapsing }

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onRatingChange: (RecipeRating) -> Unit,
    viewModel: RecipeViewModel,
    initialScrollOffset: Int = 0
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSaving by viewModel.isSaving.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()

    var editState by remember { mutableStateOf(EditBarState.Fab) }
    var showEditSheet by remember { mutableStateOf(false) }
    var promptText by remember { mutableStateOf("") }
    var isReplacingPhoto by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
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

    // FAB target dimensions
    val fabSizeDp = 64.dp
    val fabCornerDp = 20.dp
    // When fully expanded bar height = 64dp → half = 32dp = full pill radius
    val barCornerDp = 32.dp
    val fabSizePx = with(density) { fabSizeDp.toPx() }

    // Full width of the bottom container, measured after first layout pass
    var containerWidthPx by remember { mutableIntStateOf(0) }

    // Single [0,1] progress drives all morph properties
    val morphProgress = remember { Animatable(0f) }

    val fabColor = MaterialTheme.colorScheme.primaryContainer
    val barColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer

    // ── Morph spring specs (M3 Expressive) ───────────────────────────────
    // Expansion: gentle bounce so the bar "pops" open
    val expandSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 400f
    )
    // Collapse: snappier, no overshoot
    val collapseSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 580f
    )

    LaunchedEffect(editState) {
        when (editState) {
            EditBarState.Expanding -> {
                morphProgress.animateTo(1f, expandSpec)
                editState = EditBarState.Input
            }
            EditBarState.Collapsing -> {
                morphProgress.animateTo(0f, collapseSpec)
                editState = EditBarState.Fab
            }
            else -> {}
        }
    }

    // Request focus only once the morph spring has settled
    LaunchedEffect(editState) {
        if (editState == EditBarState.Input) {
            delay(60)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(initialScrollOffset) {
        if (initialScrollOffset > 0) scrollState.scrollTo(initialScrollOffset)
    }

    BackHandler {
        if (isReplacingPhoto && isSaving) {
            return@BackHandler
        }
        if (showEditSheet) {
            showEditSheet = false
            promptText = ""
        } else if (editState != EditBarState.Fab) {
            keyboardController?.hide()
            editState = EditBarState.Collapsing
            promptText = ""
        } else {
            onBack()
        }
    }

    // ── Animated values derived from morphProgress ────────────────────────
    val p = morphProgress.value
    // Width: FAB px … container px, driven by spring (with potential overshoot)
    // Clamp to [fabSizePx, containerWidthPx] so the spring bounce never clips out
    val rawWidthPx = fabSizePx + (containerWidthPx - fabSizePx) * p
    val animatedWidthPx = rawWidthPx.coerceIn(fabSizePx, containerWidthPx.toFloat().coerceAtLeast(fabSizePx))
    val animatedWidthDp: Dp = with(density) { animatedWidthPx.toDp() }

    // Corner radius: 20dp (FAB) → 32dp (pill bar)
    val animatedCornerDp: Dp = fabCornerDp + (barCornerDp - fabCornerDp) * p

    // Background colour cross-fades with the morph
    val animatedColor: Color = lerp(fabColor, barColor, p.coerceIn(0f, 1f))

    val clampedProgress = p.coerceIn(0f, 1f)
    val fabIconAlpha = (1f - (clampedProgress / 0.42f)).coerceIn(0f, 1f)
    val fabIconScale = 1f - (0.12f * clampedProgress)
    val contentAlpha = ((clampedProgress - 0.18f) / 0.52f).coerceIn(0f, 1f)
    val contentShiftPx = with(density) { ((1f - contentAlpha) * 18.dp.toPx()) }
    val capsuleElevation by animateDpAsState(
        targetValue = if (editState == EditBarState.Fab) 12.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "editBarElevation"
    )
    // ── Send action ───────────────────────────────────────────────────────
    val doSend: () -> Unit = {
        if (promptText.isNotBlank()) {
            scope.launch {
                try {
                    val result = viewModel.editRecipeWithPrompt(recipe, promptText)
                    if (result.isSuccess) {
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

    // ── Root: plain fillMaxSize Box — NO windowInsetsPadding here.
    //    Scaffold consumes no insets (contentWindowInsets = WindowInsets(0)) so
    //    the status-bar area stays transparent / full-screen.
    //    The back-button overlay handles top insets itself; the FAB overlay
    //    handles bottom + IME insets.
    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            // No topBar — back button is a floating overlay below
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                        .onSizeChanged { containerWidthPx = it.width },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val isInputPhase = editState == EditBarState.Input || editState == EditBarState.Loading
                    Box(
                        modifier = Modifier
                            .width(animatedWidthDp)
                            .then(
                                if (isInputPhase)
                                    Modifier.heightIn(min = fabSizeDp)
                                else
                                    Modifier.height(fabSizeDp)
                            )
                            .shadow(
                                elevation = capsuleElevation,
                                shape = RoundedCornerShape(animatedCornerDp),
                                spotColor = Color.Black.copy(alpha = 0.28f),
                                ambientColor = Color.Black.copy(alpha = 0.10f)
                            )
                            .clip(RoundedCornerShape(animatedCornerDp))
                            .background(animatedColor)
                            .then(
                                if (editState == EditBarState.Fab)
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) { showEditSheet = true }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editState != EditBarState.Loading) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit recipe",
                                tint = fabContentColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        alpha = fabIconAlpha
                                        scaleX = fabIconScale
                                        scaleY = fabIconScale
                                    }
                            )
                        }

                        if (editState == EditBarState.Loading) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }

                        if (editState != EditBarState.Loading && editState != EditBarState.Fab) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = promptText,
                                    onValueChange = { promptText = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    maxLines = 6,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { doSend() }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .graphicsLayer {
                                            alpha = contentAlpha
                                            translationX = contentShiftPx
                                        },
                                    decorationBox = { inner ->
                                        Box(contentAlignment = Alignment.TopStart) {
                                            if (promptText.isEmpty()) {
                                                Text(
                                                    "Describe your change…",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )

                                IconButton(
                                    onClick = doSend,
                                    enabled = promptText.isNotBlank(),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer {
                                            alpha = contentAlpha
                                            translationX = contentShiftPx * 0.7f
                                            scaleX = 0.92f + (0.08f * contentAlpha)
                                            scaleY = 0.92f + (0.08f * contentAlpha)
                                        }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = {},
                                onLongClick = { replacePhotoLauncher.launch("image/*") }
                            )
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
                            MaterialTheme.colorScheme.onSurface
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

                if (recipe.instructions.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recipe.instructions.forEach { step ->
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                RatingButtonGroup(
                    currentRating = recipe.parsedRating,
                    onRatingChange = onRatingChange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (recipe.source != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(
                            onClick = { uriHandler.openUri(recipe.source) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("View original source", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // ── Back button overlay ───────────────────────────────────────────
        // Floats at top-start, clears the status bar via windowInsetsPadding.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, top = 8.dp)
        ) {
            FilledIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
        }

        if (showEditSheet) {
            GeminiPromptBottomSheet(
                onDismissRequest = {
                    showEditSheet = false
                    promptText = ""
                },
                dismissEnabled = !isEditing,
                autoFocusPrompt = true,
                prompt = promptText,
                onPromptChange = { promptText = it },
                onSend = doSend,
                placeholder = "Describe your change…",
                sendEnabled = promptText.isNotBlank() && !isEditing
            )
        }

        if (isReplacingPhoto && isSaving) {
            PhotoSavingBottomSheet()
        }
    }
}

@Composable
private fun PhotoSavingBottomSheet() {
    GeminiBottomSheetShell(
        onDismissRequest = {},
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
                text = "Updating photo...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            var subtext by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                val messages = listOf(
                    "Saving your new hero image",
                    "Polishing the recipe card",
                    "Almost ready..."
                )
                var index = 0
                while (true) {
                    delay(2400)
                    subtext = messages[index % messages.size]
                    index++
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
private fun RatingButtonGroup(
    currentRating: RecipeRating,
    onRatingChange: (RecipeRating) -> Unit,
    modifier: Modifier = Modifier
) {
    data class RatingOption(val rating: RecipeRating, val label: String, val icon: ImageVector)

    val options = listOf(
        RatingOption(RecipeRating.TOP,  "Top",  Icons.Outlined.Favorite),
        RatingOption(RecipeRating.GOOD, "Good", Icons.Outlined.ThumbUp),
        RatingOption(RecipeRating.MID,  "Mid",  Icons.Outlined.SentimentNeutral),
        RatingOption(RecipeRating.NEW,  "New",  Icons.Outlined.QuestionMark),
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
                Icon(
                    imageVector = opt.icon,
                    contentDescription = opt.label,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                if (isSelected) {
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
