@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.jerecipes.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.theme.recipeTitleTextStyle
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt

private val SwipeSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 3500f)

@Composable
fun RecipeLibraryScreen(
    viewModel: RecipeViewModel,
    userPhotoUrl: String?,
    userEmail: String?,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPrototypeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val density = LocalDensity.current
    val toolbarHeightPx = remember { with(density) { 140.dp.toPx() } }
    var toolbarOffsetY by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarOffsetY = (toolbarOffsetY + available.y).coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                toolbarOffsetY = if (available.y > 0 || -toolbarOffsetY < toolbarHeightPx * 0.5f) {
                    0f
                } else {
                    -toolbarHeightPx
                }
                return Velocity.Zero
            }
        }
    }

    val toolbarOffset = toolbarOffsetY

    var pendingDeleteRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showBottomSheetPrototype by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayedRecipes = recipes.filter { it.id != pendingDeleteRecipe?.id }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(
                            start = 24.dp, end = 24.dp,
                            top = 24.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Prototype card — always first
                        item(key = "__prototype", span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                onClick = onPrototypeClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Fonts",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Prototype previews for expressive fonts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Settings card — always second
                        item(key = "__settings", span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                onClick = onSettingsClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Settings",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "App preferences and configuration",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        item(key = "__bottom_sheet_prototype", span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                onClick = { showBottomSheetPrototype = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ModeComment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Bottom Sheet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Prototype preview for a Gemini-style prompt sheet",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        items(displayedRecipes, key = { it.id }) { recipe ->
                            SwipeToDeleteCard(
                                onDeleteRequested = {
                                    pendingDeleteRecipe = recipe
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${recipe.title}\" deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        when (result) {
                                            SnackbarResult.ActionPerformed -> {
                                                pendingDeleteRecipe = null
                                            }
                                            SnackbarResult.Dismissed -> {
                                                viewModel.deleteRecipe(recipe.id, recipe.images)
                                                pendingDeleteRecipe = null
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                            ) {
                                RecipeCard(
                                    recipe = recipe,
                                    onClick = { onRecipeClick(recipe) }
                                )
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 108.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.3f),
                                ambientColor = Color.Black.copy(alpha = 0.1f)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            BasicTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = "Search",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Color.Black.copy(alpha = 0.4f),
                                ambientColor = Color.Black.copy(alpha = 0.15f)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onAddClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheetPrototype) {
        GeminiPromptBottomSheetPrototype(
            onDismissRequest = { showBottomSheetPrototype = false }
        )
    }
}

@Composable
fun SwipeToDeleteCard(
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var cardWidth by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    val lockPx by remember { derivedStateOf { cardWidth * 0.4f } }

    var rawOffset by remember { mutableFloatStateOf(0f) }
    val animOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val buttonWidthDp  = with(density) { lockPx.toDp() } * 0.72f
    val buttonHeightDp = 72.dp

    val progress by remember { derivedStateOf {
        val offset = if (isDragging) rawOffset else animOffset.value
        if (lockPx > 0f) (-offset / lockPx).coerceIn(0f, 1f) else 0f
    }}
    val buttonScale by remember { derivedStateOf {
        val offset = if (isDragging) rawOffset else animOffset.value
        if (lockPx > 0f) {
            val s = (-offset / lockPx).coerceAtLeast(0f)
            if (s > 1f) 1f + (s - 1f) * 0.25f else s
        } else 0f
    }}

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }
    ) {

        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            FilledIconButton(
                onClick = {
                    scope.launch { animOffset.animateTo(0f, SwipeSpring) }
                    rawOffset = 0f
                    onDeleteRequested()
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .width(buttonWidthDp)
                    .height(buttonHeightDp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                        alpha  = progress.coerceIn(0f, 1f)
                    },
                shape = RoundedCornerShape(50),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                ),
                enabled = progress >= 0.5f
            ) {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = "Delete recipe",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        val draggableState = rememberDraggableState { delta ->

            val raw = rawOffset + delta
            rawOffset = when {
                raw < -lockPx -> {
                    val over = -(raw + lockPx)
                    -(lockPx + lockPx * ln(1f + over / lockPx))
                }
                raw > 0f -> 0f
                else -> raw
            }
        }

        Box(
            modifier = Modifier
                .offset {

                    val off = if (isDragging) rawOffset else animOffset.value
                    IntOffset(off.roundToInt(), 0)
                }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        isDragging = true
                    },
                    onDragStopped = { velocity ->

                        isDragging = false
                        animOffset.snapTo(rawOffset)
                        val target = if (-rawOffset > lockPx * 0.5f) -lockPx else 0f
                        animOffset.animateTo(
                            targetValue = target,
                            animationSpec = SwipeSpring,
                            initialVelocity = velocity
                        )

                        rawOffset = animOffset.value
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                ) {
                    if (recipe.images.isNotEmpty()) {
                        AsyncImage(
                            model = recipe.images.first(),
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    val rating = recipe.parsedRating
                    if (rating != RecipeRating.NEW) {
                        Surface(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomStart),
                            shape = CircleShape,
                            color = when (rating) {
                                RecipeRating.TOP -> MaterialTheme.colorScheme.tertiaryContainer
                                RecipeRating.GOOD -> MaterialTheme.colorScheme.secondaryContainer
                                RecipeRating.MID -> MaterialTheme.colorScheme.surfaceVariant
                                else -> Color.Black.copy(alpha = 0.6f)
                            }
                        ) {
                            Text(
                                rating.name.lowercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = when (rating) {
                                    RecipeRating.TOP -> MaterialTheme.colorScheme.onTertiaryContainer
                                    RecipeRating.GOOD -> MaterialTheme.colorScheme.onSecondaryContainer
                                    RecipeRating.MID -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> Color.White
                                }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        recipe.title,
                        style = recipeTitleTextStyle(
                            MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )

                    if (!recipe.comment.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            recipe.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
