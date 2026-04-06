@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.jerecipes.ui.screens

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import com.jerecipes.ui.BoldCircularWavyProgressDefaultSize
import com.jerecipes.ui.BoldCircularWavyProgressIndicator
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.theme.FloatingBarBottomPadding
import com.jerecipes.ui.theme.FloatingBarCollapsedShadowElevation
import com.jerecipes.ui.theme.FloatingBarHeight
import com.jerecipes.ui.theme.FloatingBarHorizontalPadding
import com.jerecipes.ui.theme.FloatingBarShadowElevation
import com.jerecipes.R
import com.jerecipes.ui.theme.recipeTitleTextStyle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val SwipeSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 3500f)

private val LibraryFabCornerRadius = 22.dp

@Composable
fun RecipeLibraryScreen(
    viewModel: RecipeViewModel,
    userDisplayName: String?,
    userEmail: String?,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPrototypeClick: () -> Unit,
    onLoadingPrototypeClick: () -> Unit,
    onColorTokensClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val recipes by viewModel.libraryRecipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showBottomSheetPrototype by remember { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showPreferencesSheet by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lightSystemBarIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Modal bottom sheets can reset the activity window bar colors; re-apply edge-to-edge here.
    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightSystemBarIcons
            isAppearanceLightNavigationBars = lightSystemBarIcons
        }
    }
    val pendingDeletePair by viewModel.pendingDeleteUndo.collectAsState()
    var recipeBeingRestored by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(pendingDeletePair?.first) {
        val pair = pendingDeletePair ?: return@LaunchedEffect
        val deletedRecipe = pair.second
        try {
            when (
                snackbarHostState.showSnackbar(
                    message = "\"${deletedRecipe.title}\" deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
            ) {
                SnackbarResult.ActionPerformed -> recipeBeingRestored = deletedRecipe
                SnackbarResult.Dismissed -> viewModel.finalizeDeletedRecipeStorage(deletedRecipe.images)
            }
        } finally {
            viewModel.clearPendingDeleteUndo()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(toolbarScrollBehavior)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {},
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 12.dp + statusBarTopDp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                if (isLoading) {
                    item(key = "__loading", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BoldCircularWavyProgressIndicator(
                                modifier = Modifier.size(BoldCircularWavyProgressDefaultSize)
                            )
                        }
                    }
                } else {
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
                                    painter = painterResource(R.drawable.owl_24),
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

                    item(key = "__loading_prototype", span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            onClick = onLoadingPrototypeClick,
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
                                    Icons.Outlined.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Loading & Progress",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Prototype gallery for Material 3 indicators",
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

                    item(key = "__color_tokens", span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            onClick = onColorTokensClick,
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
                                    Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Color tokens",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Material 3 ColorScheme roles and hex values",
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

                    itemsIndexed(
                        recipes,
                        key = { _, recipe -> recipe.id }
                    ) { index, recipe ->
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset(1, 1)
                                )
                            )
                        ) {
                            SwipeReorderCard(
                                lazyGridState = gridState,
                                canMoveUp = index > 0,
                                canMoveDown = index < recipes.size - 1,
                                onMoveUp = { viewModel.moveRecipeInLibrary(recipe.id, -1) },
                                onMoveDown = { viewModel.moveRecipeInLibrary(recipe.id, 1) },
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

            }
        }

        // HorizontalFloatingToolbar's FAB slot forces a fixed size from FabSizeRange (smallest when
        // expanded=true), so Modifier.size on StandardFloatingActionButton cannot enlarge it. A Row
        // keeps the same scroll behavior on the whole group while allowing a true FloatingBarHeight FAB.
        val libraryToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors()
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = FloatingBarHorizontalPadding)
                .padding(bottom = FloatingBarBottomPadding)
                .zIndex(3f)
                .then(
                    with(toolbarScrollBehavior) {
                        Modifier.floatingScrollBehavior()
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.height(FloatingBarHeight),
                colors = libraryToolbarColors,
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = (FloatingBarHeight - 48.dp) / 2
                ),
                scrollBehavior = null,
                expandedShadowElevation = FloatingBarShadowElevation,
                collapsedShadowElevation = FloatingBarCollapsedShadowElevation,
            ) {
                LibraryFloatingToolbarPill(
                    label = "Preferences",
                    painter = painterResource(R.drawable.owl_24),
                    onClick = { showPreferencesSheet = true }
                )
                Spacer(modifier = Modifier.width(8.dp))
                LibraryFloatingToolbarPill(
                    label = "Account",
                    painter = painterResource(R.drawable.verified_user_24),
                    onClick = { showAccountSheet = true }
                )
            }
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.size(FloatingBarHeight),
                shape = RoundedCornerShape(LibraryFabCornerRadius),
                containerColor = libraryToolbarColors.fabContainerColor,
                contentColor = libraryToolbarColors.fabContentColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = FloatingBarShadowElevation,
                    pressedElevation = FloatingBarCollapsedShadowElevation,
                    focusedElevation = FloatingBarShadowElevation,
                    hoveredElevation = FloatingBarShadowElevation,
                ),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New recipe")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 108.dp)
                .zIndex(2f)
        )
    }

    if (recipeBeingRestored != null) {
        RestoreRecipeAfterDeleteBottomSheet(
            recipe = recipeBeingRestored!!,
            viewModel = viewModel,
            onFinished = { result ->
                recipeBeingRestored = null
                result.onSuccess { id ->
                    onRecipeClick(Recipe(id = id))
                }.onFailure { e ->
                    Toast.makeText(
                        context,
                        "Could not restore: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    if (showBottomSheetPrototype) {
        GeminiPromptBottomSheetPrototype(
            onDismissRequest = { showBottomSheetPrototype = false }
        )
    }

    if (showAccountSheet) {
        UserAccountBottomSheet(
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            onDismissRequest = { showAccountSheet = false },
            onLogoutClick = onLogoutClick
        )
    }

    if (showPreferencesSheet) {
        LibraryPreferencesBottomSheet(
            recipeViewModel = viewModel,
            onDismissRequest = { showPreferencesSheet = false }
        )
    }
}

@Composable
private fun RestoreRecipeAfterDeleteBottomSheet(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onFinished: (Result<String>) -> Unit
) {
    GeminiBottomSheetShell(
        onDismissRequest = {},
        dismissEnabled = false
    ) {
        Column(Modifier.fillMaxWidth()) {
            LaunchedEffect(recipe.id) {
                onFinished(viewModel.restoreRecipeAfterUndo(recipe))
            }
            GeminiSheetLoadingColumn(
                statusLine = "Restoring recipe…",
                rotatingHints = listOf(
                    "Putting it back...",
                    "Almost there...",
                    "Still working..."
                ),
                hintIntervalMillis = 2400L,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LibraryFloatingToolbarPill(
    label: String,
    painter: Painter,
    onClick: () -> Unit,
) {
    // Matches HorizontalFloatingToolbar contentPadding: inner row is 48.dp for FloatingBarHeight 68.dp.
    val toolbarSlotHeight = 48.dp
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.height(toolbarSlotHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun UserAccountBottomSheet(
    userDisplayName: String?,
    userEmail: String?,
    onDismissRequest: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        BackHandler {
            scope.launch {
                sheetState.hide()
                onDismissRequest()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = userDisplayName?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = userEmail?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onDismissRequest()
                    onLogoutClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun SwipeReorderCard(
    lazyGridState: LazyGridState,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val windowView = LocalView.current
    var cardWidth by remember { mutableIntStateOf(1) }

    val lockPx by remember { derivedStateOf { cardWidth * 0.4f } }

    var rawOffset by remember { mutableFloatStateOf(0f) }
    val animOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val buttonWidthDp = with(density) { lockPx.toDp() } * 0.72f
    val buttonHeightDp = 72.dp

    LaunchedEffect(lazyGridState) {
        combine(
            snapshotFlow { lazyGridState.isScrollInProgress },
            snapshotFlow { revealed }
        ) { scrolling, rev -> scrolling && rev }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                revealed = false
                scope.launch {
                    animOffset.animateTo(0f, SwipeSpring)
                    rawOffset = animOffset.value
                }
            }
    }

    LaunchedEffect(lockPx) {
        if (!revealed || lockPx <= 1f) return@LaunchedEffect
        val openAmount = -animOffset.value
        if (abs(openAmount - lockPx) > 4f) {
            animOffset.snapTo(-lockPx)
            rawOffset = -lockPx
        }
    }

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

    val actionsEnabled = revealed || progress >= 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }
            .onGloballyPositioned { coords ->
                if (!revealed || isDragging) return@onGloballyPositioned
                val h = coords.size.height.toFloat()
                if (h < 1f) return@onGloballyPositioned
                val bounds = coords.boundsInWindow()
                val vh = windowView.height.toFloat()
                val overlap =
                    (min(bounds.bottom, vh) - max(bounds.top, 0f)).coerceAtLeast(0f)
                val visibleFraction = overlap / h
                if (visibleFraction < 0.38f) {
                    revealed = false
                    scope.launch {
                        animOffset.animateTo(0f, SwipeSpring)
                        rawOffset = animOffset.value
                    }
                }
            }
    ) {

        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp)
                    .width(buttonWidthDp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                        alpha = progress.coerceIn(0f, 1f)
                    },
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledIconButton(
                    onClick = onMoveUp,
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .height(buttonHeightDp),
                    shape = RoundedCornerShape(50),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = actionsEnabled && canMoveUp
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move recipe up",
                        modifier = Modifier.size(32.dp)
                    )
                }
                FilledIconButton(
                    onClick = onMoveDown,
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .height(buttonHeightDp),
                    shape = RoundedCornerShape(50),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = actionsEnabled && canMoveDown
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move recipe down",
                        modifier = Modifier.size(32.dp)
                    )
                }
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
                        scope.launch {
                            animOffset.snapTo(rawOffset)
                            val target = if (-rawOffset > lockPx * 0.5f) -lockPx else 0f
                            revealed = target < 0f
                            animOffset.animateTo(
                                targetValue = target,
                                animationSpec = SwipeSpring,
                                initialVelocity = velocity
                            )
                            rawOffset = animOffset.value
                        }
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
                BoxWithConstraints(
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
                        // Match RatingButtonGroup: inner width = screen − sheet horizontal padding (30.dp × 2);
                        // selected toggle uses weight 1.5 vs 1+1+1, with ConnectedSpaceBetween × 3 between four buttons.
                        val sheetInnerWidth =
                            LocalConfiguration.current.screenWidthDp.dp - 60.dp
                        val gap = ButtonGroupDefaults.ConnectedSpaceBetween
                        val buttonRowWidth = (sheetInnerWidth - gap * 3).coerceAtLeast(0.dp)
                        val selectedSegmentWidth =
                            buttonRowWidth * (1.5f / (1.5f + 1f + 1f + 1f))
                        val pillWidth =
                            minOf(selectedSegmentWidth, maxWidth - 32.dp).coerceAtLeast(48.dp)
                        val pillColor = MaterialTheme.colorScheme.primaryContainer
                        val onPillColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val label = when (rating) {
                            RecipeRating.TOP -> "Top"
                            RecipeRating.GOOD -> "Good"
                            RecipeRating.MID -> "Mid"
                            else -> rating.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        }
                        // Match UserAccountBottomSheet "Log Out" [Button]: defaultMinSize(MinHeight) +
                        // ButtonWithIconContentPadding / ContentPadding + titleMedium (see Material3 Button.kt).
                        val buttonLikePadding =
                            if (rating == RecipeRating.TOP) {
                                ButtonDefaults.ButtonWithIconContentPadding
                            } else {
                                ButtonDefaults.ContentPadding
                            }
                        Surface(
                            modifier = Modifier
                                .width(pillWidth)
                                .wrapContentHeight()
                                .padding(16.dp)
                                .align(Alignment.BottomStart),
                            shape = ButtonGroupDefaults.connectedButtonCheckedShape,
                            color = pillColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
                                    .padding(buttonLikePadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (rating == RecipeRating.TOP) {
                                    Icon(
                                        imageVector = Icons.Outlined.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.SmallIconSize),
                                        tint = onPillColor
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onPillColor
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        recipe.title,
                        style = recipeTitleTextStyle(
                            MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )

                }
            }
        }
    }
