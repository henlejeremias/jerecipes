package com.jerecipes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Standard Material 3 Baseline Schemes (Fallback for < Android 12)
private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

// M3 Expressive: spatial spring for container transforms.
// 0.7 damping gives a visible, confident overshoot; 340 stiffness settles in ~450ms.
val ExpressiveSpring = spring<Rect>(
    dampingRatio = 0.85f,
    stiffness = 400f
)

// Crossfade specs for sharedBounds container transforms (M3 spec)
val ContainerTransformFadeIn  = tween<Float>(durationMillis = 220, delayMillis = 90)
val ContainerTransformFadeOut = tween<Float>(durationMillis = 90)

// M3 Expressive: very alive, bouncy spring for UI transitions (expand/collapse)
val ExpressiveBounceSpring = spring<Float>(
    dampingRatio = 0.55f,
    stiffness = 550f
)

// M3 Expressive: slightly damped spring for smoother spatial transitions
val ExpressiveSpatialSpring = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = 700f
)

@Composable
fun JerecipesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
            window.statusBarColor = colorScheme.surface.toArgb() // Use surface for clean app bar integration
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
