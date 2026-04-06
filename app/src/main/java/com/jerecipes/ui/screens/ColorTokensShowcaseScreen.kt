package com.jerecipes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ColorTokenSection(
    val title: String,
    val description: String,
    val tokens: List<Pair<String, Color>>
)

@Composable
fun ColorTokensShowcaseScreen() {
    val scheme = MaterialTheme.colorScheme
    val sections = remember(scheme) { colorTokenSections(scheme) }

    Scaffold(
        containerColor = scheme.background,
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
            item { ColorTokensHeroCard(scheme) }

            items(sections) { section ->
                ColorTokenSectionCard(section)
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ColorTokensHeroCard(scheme: ColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.22f),
                            scheme.tertiary.copy(alpha = 0.12f),
                            scheme.secondary.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Color tokens",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Material 3 roles from MaterialTheme.colorScheme — including dynamic color on supported devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorTokenSectionCard(section: ColorTokenSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = section.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.tokens.forEach { (name, color) ->
                    ColorTokenSwatchRow(name = name, color = color)
                }
            }
        }
    }
}

@Composable
private fun ColorTokenSwatchRow(name: String, color: Color) {
    val label = contrastingLabelColor(color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = label
        )
        Text(
            text = color.toRgbHex(),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = label.copy(alpha = 0.88f)
        )
    }
}

private fun contrastingLabelColor(background: Color): Color {
    val luminance =
        0.2126f * srgbChannel(background.red) +
            0.7152f * srgbChannel(background.green) +
            0.0722f * srgbChannel(background.blue)
    return if (luminance > 0.45f) Color(0xFF1C1B1F).copy(alpha = 0.92f)
    else Color(0xFFF4EFF4).copy(alpha = 0.95f)
}

private fun srgbChannel(c: Float): Float {
    val x = c.coerceIn(0f, 1f)
    return if (x <= 0.03928f) x / 12.92f
    else Math.pow(((x + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
}

private fun Color.toRgbHex(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

private fun colorTokenSections(scheme: ColorScheme): List<ColorTokenSection> = listOf(
    ColorTokenSection(
        title = "Primary",
        description = "Brand emphasis, key actions, and prominent fills.",
        tokens = listOf(
            "primary" to scheme.primary,
            "onPrimary" to scheme.onPrimary,
            "primaryContainer" to scheme.primaryContainer,
            "onPrimaryContainer" to scheme.onPrimaryContainer,
            "inversePrimary" to scheme.inversePrimary
        )
    ),
    ColorTokenSection(
        title = "Secondary",
        description = "Accents less prominent than primary.",
        tokens = listOf(
            "secondary" to scheme.secondary,
            "onSecondary" to scheme.onSecondary,
            "secondaryContainer" to scheme.secondaryContainer,
            "onSecondaryContainer" to scheme.onSecondaryContainer
        )
    ),
    ColorTokenSection(
        title = "Tertiary",
        description = "Contrast and balance against primary and secondary.",
        tokens = listOf(
            "tertiary" to scheme.tertiary,
            "onTertiary" to scheme.onTertiary,
            "tertiaryContainer" to scheme.tertiaryContainer,
            "onTertiaryContainer" to scheme.onTertiaryContainer
        )
    ),
    ColorTokenSection(
        title = "Fixed palettes",
        description = "Stable tones for components that should not shift heavily with surface level.",
        tokens = listOf(
            "primaryFixed" to scheme.primaryFixed,
            "primaryFixedDim" to scheme.primaryFixedDim,
            "onPrimaryFixed" to scheme.onPrimaryFixed,
            "onPrimaryFixedVariant" to scheme.onPrimaryFixedVariant,
            "secondaryFixed" to scheme.secondaryFixed,
            "secondaryFixedDim" to scheme.secondaryFixedDim,
            "onSecondaryFixed" to scheme.onSecondaryFixed,
            "onSecondaryFixedVariant" to scheme.onSecondaryFixedVariant,
            "tertiaryFixed" to scheme.tertiaryFixed,
            "tertiaryFixedDim" to scheme.tertiaryFixedDim,
            "onTertiaryFixed" to scheme.onTertiaryFixed,
            "onTertiaryFixedVariant" to scheme.onTertiaryFixedVariant
        )
    ),
    ColorTokenSection(
        title = "Background & surface",
        description = "Base canvas, default surfaces, and tonal surface variants.",
        tokens = listOf(
            "background" to scheme.background,
            "onBackground" to scheme.onBackground,
            "surface" to scheme.surface,
            "onSurface" to scheme.onSurface,
            "surfaceVariant" to scheme.surfaceVariant,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "surfaceTint" to scheme.surfaceTint
        )
    ),
    ColorTokenSection(
        title = "Surface containers",
        description = "Layered elevation through container steps (dim → highest).",
        tokens = listOf(
            "surfaceDim" to scheme.surfaceDim,
            "surfaceBright" to scheme.surfaceBright,
            "surfaceContainerLowest" to scheme.surfaceContainerLowest,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest
        )
    ),
    ColorTokenSection(
        title = "Inverse",
        description = "Inverted surfaces for snackbars, dialogs, and inverse layouts.",
        tokens = listOf(
            "inverseSurface" to scheme.inverseSurface,
            "inverseOnSurface" to scheme.inverseOnSurface
        )
    ),
    ColorTokenSection(
        title = "Error",
        description = "Destructive states and validation.",
        tokens = listOf(
            "error" to scheme.error,
            "onError" to scheme.onError,
            "errorContainer" to scheme.errorContainer,
            "onErrorContainer" to scheme.onErrorContainer
        )
    ),
    ColorTokenSection(
        title = "Borders & overlay",
        description = "Dividers, focus rings, and modal scrims.",
        tokens = listOf(
            "outline" to scheme.outline,
            "outlineVariant" to scheme.outlineVariant,
            "scrim" to scheme.scrim
        )
    )
)
