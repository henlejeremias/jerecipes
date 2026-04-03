package com.jerecipes.ui.theme

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

val ExpressiveSpring = spring<Rect>(
    dampingRatio = 0.85f,
    stiffness = 400f
)

val ContainerTransformFadeIn  = tween<Float>(durationMillis = 220, delayMillis = 90)
val ContainerTransformFadeOut = tween<Float>(durationMillis = 90)

val ExpressiveBounceSpring = spring<Float>(
    dampingRatio = 0.55f,
    stiffness = 550f
)

val ExpressiveSpatialSpring = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = 700f
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


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
