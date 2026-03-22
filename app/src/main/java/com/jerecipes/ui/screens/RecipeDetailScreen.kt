@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jerecipes.data.model.Recipe
import com.jerecipes.ui.theme.ExpressiveSpring

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .sharedElement(
                    rememberSharedContentState(key = "card-${recipe.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> ExpressiveSpring }
                )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    AsyncImage(
                        model = recipe.images.firstOrNull(),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "image-${recipe.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> ExpressiveSpring }
                            )
                            .clip(RoundedCornerShape(bottomStart = 64.dp, bottomEnd = 64.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (recipe.calories != null) {
                        Surface(
                            modifier = Modifier
                                .padding(32.dp)
                                .align(Alignment.BottomStart),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.9f)
                        ) {
                            Text(
                                "${recipe.calories} CAL",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp, vertical = 40.dp)
                ) {
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = 48.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "title-${recipe.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> ExpressiveSpring }
                        )
                    )

                    Spacer(Modifier.height(32.dp))

                    // Quick Info Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoChip(icon = Icons.Default.Restaurant, text = "${recipe.ingredients.size} INGREDIENTS")
                        if (recipe.calories != null) {
                            InfoChip(icon = Icons.Default.Schedule, text = "${recipe.calories} CAL")
                        }
                        if (recipe.source != null) {
                            InfoChip(icon = Icons.Default.Group, text = "HAS SOURCE")
                        }
                    }

                    if (recipe.comment != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            recipe.comment!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 28.sp,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    // Ingredients Section
                    Text(
                        "Ingredients",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(24.dp))

                    if (recipe.ingredients.isEmpty()) {
                        Text("No ingredients listed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        recipe.ingredients.forEach { ingredient ->
                            IngredientRow(
                                name = ingredient.name,
                                amount = ingredient.amount,
                                unit = ingredient.unit
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    // Instructions Section
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        recipe.instructions,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(48.dp))

                    Button(
                        onClick = { /* Start Cooking */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            "Start Cooking",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    Spacer(Modifier.height(48.dp))
                }
            }

            // Top Buttons
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                IconButton(
                    onClick = { /* Favorite */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun IngredientRow(name: String, amount: Double?, unit: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        val amountText = if (amount != null) {
            val formattedAmount = if (amount == amount.toInt().toDouble()) amount.toInt().toString() else amount.toString()
            "$formattedAmount${unit ?: ""} "
        } else ""
        Text(
            text = "$amountText$name",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
