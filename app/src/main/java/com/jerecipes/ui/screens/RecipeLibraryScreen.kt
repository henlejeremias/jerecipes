@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jerecipes.data.model.Recipe
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.theme.ExpressiveSpring
import com.jerecipes.ui.theme.JerecipesTheme

@Composable
fun RecipeLibraryScreen(
    viewModel: RecipeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRecipeClick: (Recipe) -> Unit,
    onAddClick: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Jerecipes",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCwC0kec9zr2ufP_tQYuz9_KLaWMAKcDPQ8YDrU_qZMgomBjlriu4zOHY720EwHKwcioiwSpl78PFrFsGNfYWmeq7LxDBbr_3buoBxeISnbQjgzzouyEHC6ECvsyJalhQi9QbE-qZtM7xf9Z0LoTk23BMdjv89qUCFHBuweIJU0WgP95wKASoEBwaR7GDjFddcFPsNg77OwRNV6jAptW3XI6xgR0w_OXhkIw2fmXVK3zLS8hM36RwhUNJXcMwAIVVRVFkWk5NqQKe4",
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Recipe",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Bar
            RecipeSearchBar(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )

            // Recipe Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recipes) { recipe ->
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

@Composable
fun RecipeSearchBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 0.dp // Going for flat expressive look
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Find a recipe for dinner tonight...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

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
                .sharedElement(
                    rememberSharedContentState(key = "card-${recipe.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> ExpressiveSpring }
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column {
                Box {
                    AsyncImage(
                        model = recipe.images.firstOrNull(),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 5f)
                            .sharedElement(
                                rememberSharedContentState(key = "image-${recipe.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> ExpressiveSpring }
                            )
                            .clip(RoundedCornerShape(32.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (recipe.calories != null) {
                        Surface(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomStart),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.9f)
                        ) {
                            Text(
                                "${recipe.calories} CAL",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "title-${recipe.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> ExpressiveSpring }
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        recipe.comment ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RecipeLibraryPreview() {
    JerecipesTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                RecipeLibraryScreen(
                    viewModel = RecipeViewModel(),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    onRecipeClick = {},
                    onAddClick = {}
                )
            }
        }
    }
}
