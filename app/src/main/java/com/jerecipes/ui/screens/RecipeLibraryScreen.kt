@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Delete // Keeping this for reference or replacing below
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.jerecipes.ui.theme.ContainerTransformFadeIn
import com.jerecipes.ui.theme.ContainerTransformFadeOut
import com.jerecipes.ui.theme.ExpressiveSpring
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt

// ── Swipe-to-delete spring ───────────────────────────────────────────────────
// Controlled bounce — enough overshoot to feel alive, not wild.
// Extremely snappy and mechanical — instantaneous return with a sharp, high-frequency bounce.
private val SwipeSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 3500f)

@Composable
fun RecipeLibraryScreen(
    viewModel: RecipeViewModel,
    userPhotoUrl: String?,
    userEmail: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPrototypeClick: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // --- Scroll-to-hide state ---
    val density = LocalDensity.current
    val toolbarHeightPx = remember { with(density) { 140.dp.toPx() } }
    var toolbarOffsetY by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarOffsetY = (toolbarOffsetY + available.y).coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }

            // Snap fully show/hide on fling — no half-hidden states
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


    // M3 Expressive: gentle snap spring for scroll-hide (no bounce needed here)
    val toolbarOffsetAnimated by animateFloatAsState(
        targetValue = toolbarOffsetY,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 380f),
        label = "toolbarOffset"
    )

    // --- Swipe-to-delete state ---
    var pendingDeleteRecipe by remember { mutableStateOf<Recipe?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayedRecipes = recipes.filter { it.id != pendingDeleteRecipe?.id }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Scrollable Content ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                Text(
                    text = "JERECIPES",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // ── Prototype Entry ─────────────────────────
                Card(
                    onClick = { onPrototypeClick() },
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Prototype",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Redirects to a prototype page if available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(
                            start = 24.dp, end = 24.dp,
                            top = 24.dp, bottom = 160.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                modifier = Modifier.animateItem()
                            ) {
                                RecipeCard(
                                    recipe = recipe,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { onRecipeClick(recipe) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Snackbar Host — above the floating toolbar ───────────────
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 108.dp)
            )

            // ── Floating Toolbar Overlay ─────────────────────────────────

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    // safeDrawing tracks whichever is larger: keyboard or nav bar
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -toolbarOffsetAnimated.roundToInt()
                        )
                    }
                    .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    // 1. Action Pill (static — search is disabled for now)
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .wrapContentWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Search icon — no-op for now
                            IconButton(onClick = { /* TODO: search */ }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { /* TODO: Info */ }) {
                                Icon(Icons.Outlined.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { /* TODO: Settings */ }) {
                                Icon(Icons.Outlined.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = onLogoutClick) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 2. Docked FAB
                    Box(
                        modifier = Modifier
                            .size(64.dp)
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
}

// ── Swipe-to-Delete Wrapper ─────────────────────────────────────────────────
// Drag uses a plain float (no coroutines → no race).
// Animatable fires only on release, with the gesture's velocity for M3 Expressive feel.
@Composable
fun SwipeToDeleteCard(
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var cardWidth by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    // 40% of card width is the snap-to-locked threshold
    val lockPx by remember { derivedStateOf { cardWidth * 0.4f } }

    // ── Drag vs animation offset split ──────────────────────────────
    // During drag: rawOffset is the source of truth (plain state, no coroutines).
    // On release: animOffset runs the spring; isDragging flips to show it.
    var rawOffset by remember { mutableFloatStateOf(0f) }
    val animOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Wide pill button sized within the revealed zone
    val buttonWidthDp  = with(density) { lockPx.toDp() } * 0.72f
    val buttonHeightDp = 72.dp

    // ── Key jitter fix ───────────────────────────────────────────────
    // progress/buttonScale are read during *composition* (needed for the
    // button's graphicsLayer). They legitimately recompose when the drag
    // moves through threshold. But displayOffset is ONLY needed in
    // offset{} which runs in the *layout* phase — so we read it there
    // directly, saving a full recomposition per drag delta.
    val progress by remember { derivedStateOf {
        val offset = if (isDragging) rawOffset else animOffset.value
        if (lockPx > 0f) (-offset / lockPx).coerceIn(0f, 1f) else 0f
    }}
    val buttonScale by remember { derivedStateOf {
        val offset = if (isDragging) rawOffset else animOffset.value
        if (lockPx > 0f) {
            val s = (-offset / lockPx).coerceAtLeast(0f)
            if (s > 1f) 1f + (s - 1f) * 0.25f else s // Subtle growth past lock point
        } else 0f
    }}

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }
    ) {
        // ── Delete button — floats behind the card, no backdrop ──────
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

        // ── Foreground: the actual card ──────────────────────────────
        val draggableState = rememberDraggableState { delta ->
            // Plain state mutation — no coroutine, no race
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
                    // Read directly in layout lambda — no recomposition on drag deltas
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
                        // Hand off from raw → Animatable, with gesture velocity
                        isDragging = false
                        animOffset.snapTo(rawOffset)
                        val target = if (-rawOffset > lockPx * 0.5f) -lockPx else 0f
                        animOffset.animateTo(
                            targetValue = target,
                            animationSpec = SwipeSpring,
                            initialVelocity = velocity
                        )
                        // Sync raw back so next drag starts from where animation landed
                        rawOffset = animOffset.value
                    }
                )
        ) {
            content()
        }
    }
}

// ── Recipe Card ─────────────────────────────────────────────────────────────
@Composable
fun RecipeCard(
    recipe: Recipe,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .sharedBounds(
                    rememberSharedContentState(key = "card-${recipe.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> ExpressiveSpring },
                    enter = fadeIn(ContainerTransformFadeIn),
                    exit = fadeOut(ContainerTransformFadeOut),
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(24.dp))
                ).clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                                .fillMaxSize()
                                .sharedElement(
                                    rememberSharedContentState(key = "image-${recipe.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> ExpressiveSpring }
                                ),
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
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "title-${recipe.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> ExpressiveSpring }
                        )
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
}
