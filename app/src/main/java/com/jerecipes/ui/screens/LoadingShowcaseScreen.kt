@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.jerecipes.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LoadingShowcaseScreen() {
    val animatedProgress by rememberInfiniteTransition(label = "loadingShowcase")
        .animateFloat(
            initialValue = 0.08f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "progress"
        )
    val visibleRingTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCard()
            }

            item {
                ShowcaseSectionCard(
                    title = "Contained Loading",
                    description = "Regular contained loading indicator variants shown side by side."
                ) {
                    VariantGroup(
                        title = "Contained Loading",
                        caption = "Same component compared by size, container treatment, and determinate state."
                    ) {
                        VariantRow(
                            VariantSpec("Default", "Indeterminate") {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            },
                            VariantSpec("Larger", "Bolder container") {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(72.dp),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    containerShape = RoundedCornerShape(22.dp)
                                )
                            },
                            VariantSpec("Determinate", "Primary container") {
                                ContainedLoadingIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(64.dp),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerShape = RoundedCornerShape(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            item {
                ShowcaseSectionCard(
                    title = "Wavy Progress",
                    description = "Circular wavy progress indicators with the static track ring made intentionally visible."
                ) {
                    VariantGroup(
                        title = "Circular Wavy, Indeterminate",
                        caption = "Each variant keeps the static ring visible behind the moving wave."
                    ) {
                        VariantRow(
                            VariantSpec("Calm", "Light wave") {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(52.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = visibleRingTrackColor,
                                    amplitude = 0.18f,
                                    wavelength = 18.dp,
                                    waveSpeed = 18.dp
                                )
                            },
                            VariantSpec("Balanced", "Medium weight") {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(58.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = visibleRingTrackColor,
                                    stroke = Stroke(width = 8f, cap = StrokeCap.Round),
                                    trackStroke = Stroke(width = 8f, cap = StrokeCap.Round),
                                    amplitude = 0.32f,
                                    wavelength = 14.dp,
                                    waveSpeed = 22.dp
                                )
                            },
                            VariantSpec("Bold", "Fast + chunky") {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(72.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = visibleRingTrackColor,
                                    stroke = Stroke(width = 12f, cap = StrokeCap.Round),
                                    trackStroke = Stroke(width = 12f, cap = StrokeCap.Round),
                                    amplitude = 0.62f,
                                    wavelength = 16.dp,
                                    waveSpeed = 38.dp
                                )
                            }
                        )
                    }

                    VariantGroup(
                        title = "Circular Wavy, Determinate",
                        caption = "Determinate variants with a clearly visible static ring for easier comparison."
                    ) {
                        VariantRow(
                            VariantSpec("Thin", "Subtle amplitude") {
                                CircularWavyProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(52.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = visibleRingTrackColor,
                                    stroke = Stroke(width = 5f, cap = StrokeCap.Round),
                                    trackStroke = Stroke(width = 5f, cap = StrokeCap.Round),
                                    amplitude = { 0.14f }
                                )
                            },
                            VariantSpec("Balanced", "Reactive amplitude") {
                                CircularWavyProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(58.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = visibleRingTrackColor,
                                    stroke = Stroke(width = 8f, cap = StrokeCap.Round),
                                    trackStroke = Stroke(width = 8f, cap = StrokeCap.Round),
                                    amplitude = { progress -> 0.12f + (progress * 0.35f) }
                                )
                            },
                            VariantSpec("Heavy", "Higher contrast") {
                                CircularWavyProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(72.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = visibleRingTrackColor,
                                    stroke = Stroke(width = 11f, cap = StrokeCap.Round),
                                    trackStroke = Stroke(width = 11f, cap = StrokeCap.Round),
                                    amplitude = { progress -> 0.18f + (progress * 0.38f) }
                                )
                            }
                        )
                    }
                }
            }

            item {
                ShowcaseSectionCard(
                    title = "Expressive Loading",
                    description = "Morphing loaders grouped by the same component so shape families are easy to compare."
                ) {
                    VariantGroup(
                        title = "Loading Indicator",
                        caption = "Same loader, different polygon families next to each other."
                    ) {
                        VariantRow(
                            VariantSpec("Default", "Built-in set") {
                                LoadingIndicator(
                                    modifier = Modifier.size(56.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            VariantSpec("Geometric", "Circle, diamond, oval") {
                                LoadingIndicator(
                                    modifier = Modifier.size(68.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    polygons = listOf(
                                        MaterialShapes.Circle,
                                        MaterialShapes.Diamond,
                                        MaterialShapes.Oval,
                                        MaterialShapes.Burst
                                    )
                                )
                            },
                            VariantSpec("Organic", "Heart, flower, clover") {
                                LoadingIndicator(
                                    modifier = Modifier.size(68.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    polygons = listOf(
                                        MaterialShapes.Circle,
                                        MaterialShapes.Heart,
                                        MaterialShapes.Flower,
                                        MaterialShapes.Clover4Leaf
                                    )
                                )
                            }
                        )
                    }

                    VariantGroup(
                        title = "Contained Loading",
                        caption = "Container shape and polygon set comparisons."
                    ) {
                        VariantRow(
                            VariantSpec("Rounded", "Default-ish") {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(64.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerShape = RoundedCornerShape(18.dp)
                                )
                            },
                            VariantSpec("Pill", "Geometric shapes") {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(76.dp),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    containerShape = RoundedCornerShape(28.dp),
                                    polygons = listOf(
                                        MaterialShapes.Circle,
                                        MaterialShapes.Diamond,
                                        MaterialShapes.Oval
                                    )
                                )
                            },
                            VariantSpec("Squircle", "Determinate + organic") {
                                ContainedLoadingIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(72.dp),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerShape = RoundedCornerShape(22.dp),
                                    polygons = listOf(
                                        MaterialShapes.Circle,
                                        MaterialShapes.Heart,
                                        MaterialShapes.Flower
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Loading + Progress",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A quick gallery of Material 3 standard and expressive indicators.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f)
                )
            }
        }
    }
}

@Composable
private fun ShowcaseSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun VariantGroup(
    title: String,
    caption: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun VariantRow(
    first: VariantSpec,
    second: VariantSpec,
    third: VariantSpec
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - 24.dp) / 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VariantTile(first, Modifier.width(tileWidth))
            VariantTile(second, Modifier.width(tileWidth))
            VariantTile(third, Modifier.width(tileWidth))
        }
    }
}

private data class VariantSpec(
    val title: String,
    val caption: String,
    val content: @Composable () -> Unit
)

@Composable
private fun VariantTile(
    spec: VariantSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                spec.content()
            }
            Text(
                text = spec.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = spec.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
