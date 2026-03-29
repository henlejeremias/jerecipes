@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.jerecipes.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.jerecipes.data.model.Ingredient
import com.jerecipes.data.model.Recipe
import com.jerecipes.ui.theme.FrauncesFontFamily
import java.util.UUID

// ─── Wrapper with stable ID for drag-and-drop ─────────────────────────────────

private data class Identified<T>(val id: String = UUID.randomUUID().toString(), val value: T)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    isSaving: Boolean,
    onSave: (Recipe, Bitmap?, List<String>, Int) -> Unit,
    onBack: () -> Unit
) {
    // Intercept system back button to trigger the same logic as the Close button
    BackHandler { onBack() }

    // ── Editable state ────────────────────────────────────────────────────────
    var title by remember { mutableStateOf(recipe.title) }
    var comment by remember { mutableStateOf(recipe.comment ?: "") }
    var source by remember { mutableStateOf(recipe.source ?: "") }
    var ingredients by remember {
        mutableStateOf(recipe.ingredients.map { Identified(value = it) })
    }
    var steps by remember {
        mutableStateOf(recipe.instructions.map { Identified(value = it) })
    }

    // ── Image state ───────────────────────────────────────────────────────────
    var existingImages by remember { mutableStateOf(recipe.images) }
    var imagesToDelete by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                // Track old images for deletion, then replace
                imagesToDelete = imagesToDelete + existingImages
                existingImages = emptyList()
                selectedBitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Drag state (shared for both sections) ─────────────────────────────────
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }

    val scrollState = rememberScrollState()
    val blurRadius by animateDpAsState(
        targetValue = if (isSaving) 32.dp else 0.dp,
        animationSpec = spring(stiffness = 500f),
        label = "blur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.blur(blurRadius),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = !isSaving,
                        enter = fadeIn(spring(stiffness = 600f)),
                        exit = fadeOut(spring(stiffness = 600f))
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val updatedRecipe = recipe.copy(
                                    title = title,
                                    instructions = steps.map { it.value }.filter { it.isNotBlank() },
                                    comment = comment.ifBlank { null },
                                    ingredients = ingredients.map { it.value }.filter { it.name.isNotBlank() },
                                    source = source.ifBlank { null },
                                    images = existingImages
                                )
                                onSave(updatedRecipe, selectedBitmap, imagesToDelete, scrollState.value)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
        ) {
            // ── Hero image (tappable to replace photo) ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clickable { launcher.launch("image/*") }
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (existingImages.isNotEmpty()) {
                    AsyncImage(
                        model = existingImages.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.AddAPhoto,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tap to add photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Gradient overlay (matches detail view)
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



                // Editable title (overlaid, same position as detail view)
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .fillMaxWidth(0.85f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    "Recipe title",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = FrauncesFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // ── Comment (subtle editable field below title) ───────────────────
            BasicTextField(
                value = comment,
                onValueChange = { comment = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (comment.isEmpty()) {
                            Text(
                                "Add a personal note…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // ── Carousel (read-only, matches detail view) ────────────────────
            Spacer(Modifier.height(8.dp))
            RecipeFactsCarousel(recipe = recipe)

            // ── Ingredients (editable, reorderable) ──────────────────────────
            if (ingredients.isNotEmpty() || true) { // Always show in edit mode
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val currentIngredients by rememberUpdatedState(ingredients)
                    val haptic = LocalHapticFeedback.current
                    val density = LocalDensity.current
                    val spacingPx = with(density) { 10.dp.toPx() }

                    ingredients.forEachIndexed { index, item ->
                        key(item.id) {
                            val isDragging = draggingId == item.id
                            EditableIngredientPill(
                                ingredient = item.value,
                                isDragging = isDragging,
                                dragOffset = if (isDragging) dragOffset else 0f,
                                onUpdate = { updated ->
                                    ingredients = ingredients.toMutableList().apply {
                                        this[index] = item.copy(value = updated)
                                    }
                                },
                                onDelete = {
                                    ingredients = ingredients.toMutableList().apply { removeAt(index) }
                                },
                                onSizeChanged = { h -> itemHeights[item.id] = h },
                                dragHandleModifier = Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingId = item.id
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            val curIdx = currentIngredients.indexOfFirst { it.id == item.id }
                                            if (curIdx == -1) return@detectDragGestures
                                            val h = (itemHeights[item.id] ?: return@detectDragGestures).toFloat() + spacingPx
                                            if (dragOffset > h * 0.5f && curIdx < currentIngredients.size - 1) {
                                                val next = currentIngredients[curIdx + 1]
                                                ingredients = currentIngredients.toMutableList().apply {
                                                    add(curIdx + 1, removeAt(curIdx))
                                                }
                                                dragOffset -= (itemHeights[next.id]?.toFloat() ?: h) + spacingPx
                                            } else if (dragOffset < -h * 0.5f && curIdx > 0) {
                                                val prev = currentIngredients[curIdx - 1]
                                                ingredients = currentIngredients.toMutableList().apply {
                                                    add(curIdx - 1, removeAt(curIdx))
                                                }
                                                dragOffset += (itemHeights[prev.id]?.toFloat() ?: h) + spacingPx
                                            }
                                        },
                                        onDragEnd = { draggingId = null; dragOffset = 0f },
                                        onDragCancel = { draggingId = null; dragOffset = 0f }
                                    )
                                }
                            )
                        }
                    }

                    // Add Ingredient button
                    Surface(
                        onClick = { ingredients = ingredients + Identified(value = Ingredient("")) },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Add ingredient", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ── Instructions (editable, reorderable) ────────────────────────
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val currentSteps by rememberUpdatedState(steps)
                val haptic = LocalHapticFeedback.current
                val density = LocalDensity.current
                val spacingPx = with(density) { 10.dp.toPx() }

                steps.forEachIndexed { index, item ->
                    key(item.id) {
                        val isDragging = draggingId == item.id
                        EditableInstructionCard(
                            step = item.value,
                            isDragging = isDragging,
                            dragOffset = if (isDragging) dragOffset else 0f,
                            onUpdate = { updated ->
                                steps = steps.toMutableList().apply {
                                    this[index] = item.copy(value = updated)
                                }
                            },
                            onDelete = {
                                steps = steps.toMutableList().apply { removeAt(index) }
                            },
                            onSizeChanged = { h -> itemHeights[item.id] = h },
                            dragHandleModifier = Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingId = item.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val curIdx = currentSteps.indexOfFirst { it.id == item.id }
                                        if (curIdx == -1) return@detectDragGestures
                                        val h = (itemHeights[item.id] ?: return@detectDragGestures).toFloat() + spacingPx
                                        if (dragOffset > h * 0.5f && curIdx < currentSteps.size - 1) {
                                            val next = currentSteps[curIdx + 1]
                                            steps = currentSteps.toMutableList().apply {
                                                add(curIdx + 1, removeAt(curIdx))
                                            }
                                            dragOffset -= (itemHeights[next.id]?.toFloat() ?: h) + spacingPx
                                        } else if (dragOffset < -h * 0.5f && curIdx > 0) {
                                            val prev = currentSteps[curIdx - 1]
                                            steps = currentSteps.toMutableList().apply {
                                                add(curIdx - 1, removeAt(curIdx))
                                            }
                                            dragOffset += (itemHeights[prev.id]?.toFloat() ?: h) + spacingPx
                                        }
                                    },
                                    onDragEnd = { draggingId = null; dragOffset = 0f },
                                    onDragCancel = { draggingId = null; dragOffset = 0f }
                                )
                            }
                        )
                    }
                }

                // Add Step button
                Surface(
                    onClick = { steps = steps + Identified(value = "") },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add step", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ── Source (editable) ────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = source,
                        onValueChange = { source = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (source.isEmpty()) {
                                    Text(
                                        "Original source URL",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            // Bottom spacer
            Spacer(Modifier.height(100.dp))
        }
    }

    // ── Save blur overlay ─────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = isSaving,
        enter = fadeIn(spring(stiffness = 600f)),
        exit = fadeOut(spring(stiffness = 600f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(
                modifier = Modifier.size(64.dp)
            )
        }
    }
    } // end Box
}

// ─── Editable Ingredient Pill (mirrors detail view IngredientPillRow) ─────────

@Composable
private fun EditableIngredientPill(
    ingredient: Ingredient,
    isDragging: Boolean,
    dragOffset: Float,
    onUpdate: (Ingredient) -> Unit,
    onDelete: () -> Unit,
    onSizeChanged: (Int) -> Unit,
    dragHandleModifier: Modifier
) {
    val outerHeight = 64.dp
    val innerHeight = 44.dp
    val concentricPadding = (outerHeight - innerHeight) / 2

    // Build amount+unit display string
    val amountUnitText = remember(ingredient.amount, ingredient.unit) {
        buildString {
            ingredient.amount?.let {
                append(if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString())
            }
            if (ingredient.amount != null && !ingredient.unit.isNullOrEmpty()) append(" ")
            ingredient.unit?.let { append(it) }
        }
    }
    var amountUnitField by remember(ingredient) { mutableStateOf(amountUnitText) }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "elevation"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = outerHeight)
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = dragOffset
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .onSizeChanged { onSizeChanged(it.height) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = concentricPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = dragHandleModifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )

            // Ingredient name (editable, matches detail view style)
            BasicTextField(
                value = ingredient.name,
                onValueChange = { onUpdate(ingredient.copy(name = it)) },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                singleLine = true,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (ingredient.name.isEmpty()) {
                            Text(
                                "Ingredient",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        inner()
                    }
                }
            )

            // Inner concentric pill for amount/unit (editable)
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .height(innerHeight)
                    .widthIn(min = 72.dp)
            ) {
                BasicTextField(
                    value = amountUnitField,
                    onValueChange = { newVal ->
                        amountUnitField = newVal
                        val parts = newVal.trim().split(" ", limit = 2)
                        val amt = parts.getOrNull(0)?.toDoubleOrNull()
                        val unit = parts.getOrNull(1) ?: (if (amt == null && newVal.isNotBlank()) newVal else "")
                        onUpdate(ingredient.copy(amount = amt, unit = unit.ifBlank { null }))
                    },
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(horizontal = 14.dp),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            if (amountUnitField.isEmpty()) {
                                Text(
                                    "Qty",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Editable Instruction Card (mirrors detail view instruction cards) ────────

@Composable
private fun EditableInstructionCard(
    step: String,
    isDragging: Boolean,
    dragOffset: Float,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit,
    onSizeChanged: (Int) -> Unit,
    dragHandleModifier: Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "elevation"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = dragOffset
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .onSizeChanged { onSizeChanged(it.height) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Drag handle
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f),
                modifier = dragHandleModifier
                    .size(24.dp)
                    .padding(top = 4.dp)
            )

            Spacer(Modifier.width(8.dp))

            // Step text (editable)
            BasicTextField(
                value = step,
                onValueChange = onUpdate,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).padding(top = 4.dp),
                decorationBox = { inner ->
                    Box {
                        if (step.isEmpty()) {
                            Text(
                                "Describe this step…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                }
            )

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}
