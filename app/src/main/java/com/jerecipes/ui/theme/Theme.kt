package com.jerecipes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = JerecipesPrimaryDark,
    onPrimary = JerecipesOnPrimaryDark,
    primaryContainer = JerecipesPrimaryContainerDark,
    onPrimaryContainer = JerecipesOnPrimaryContainerDark,
    secondary = JerecipesSecondaryDark,
    onSecondary = JerecipesOnSecondaryDark,
    secondaryContainer = JerecipesSecondaryContainerDark,
    onSecondaryContainer = JerecipesOnSecondaryContainerDark,
    tertiary = JerecipesTertiaryDark,
    onTertiary = JerecipesOnTertiaryDark,
    tertiaryContainer = JerecipesTertiaryContainerDark,
    onTertiaryContainer = JerecipesOnTertiaryContainerDark,
    background = JerecipesBackgroundDark,
    onBackground = JerecipesOnBackgroundDark,
    surface = JerecipesSurfaceDark,
    onSurface = JerecipesOnSurfaceDark,
    surfaceVariant = JerecipesSurfaceVariantDark,
    onSurfaceVariant = JerecipesOnSurfaceVariantDark,
    surfaceContainerLowest = JerecipesSurfaceContainerLowestDark,
    surfaceContainerLow = JerecipesSurfaceContainerLowDark,
    surfaceContainer = JerecipesSurfaceContainerDark,
    surfaceContainerHigh = JerecipesSurfaceContainerHighDark,
    surfaceContainerHighest = JerecipesSurfaceContainerHighestDark,
    surfaceBright = JerecipesSurfaceBrightDark
)

private val LightColorScheme = lightColorScheme(
    primary = JerecipesPrimary,
    onPrimary = JerecipesOnPrimary,
    primaryContainer = JerecipesPrimaryContainer,
    onPrimaryContainer = JerecipesOnPrimaryContainer,
    secondary = JerecipesSecondary,
    onSecondary = JerecipesOnSecondary,
    secondaryContainer = JerecipesSecondaryContainer,
    onSecondaryContainer = JerecipesOnSecondaryContainer,
    tertiary = JerecipesTertiary,
    onTertiary = JerecipesOnTertiary,
    tertiaryContainer = JerecipesTertiaryContainer,
    onTertiaryContainer = JerecipesOnTertiaryContainer,
    background = JerecipesBackground,
    onBackground = JerecipesOnBackground,
    surface = JerecipesSurface,
    onSurface = JerecipesOnSurface,
    surfaceVariant = JerecipesSurfaceVariant,
    onSurfaceVariant = JerecipesOnSurfaceVariant,
    surfaceContainerLowest = JerecipesSurfaceContainerLowest,
    surfaceContainerLow = JerecipesSurfaceContainerLow,
    surfaceContainer = JerecipesSurfaceContainer,
    surfaceContainerHigh = JerecipesSurfaceContainerHigh,
    surfaceContainerHighest = JerecipesSurfaceContainerHighest,
    surfaceBright = JerecipesSurfaceBright
)

// Material 3 Expressive Spring Spec: Low stiffness, no bounce for a "premium" feel
val ExpressiveSpring = spring<Rect>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

@Composable
fun JerecipesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
