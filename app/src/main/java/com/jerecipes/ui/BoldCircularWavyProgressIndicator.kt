@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.jerecipes.ui

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val BoldCircularWavyProgressDefaultSize: Dp = 80.dp

@Composable
fun BoldCircularWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    CircularWavyProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        stroke = Stroke(width = 12f, cap = StrokeCap.Round),
        trackStroke = Stroke(width = 12f, cap = StrokeCap.Round),
        amplitude = 0.62f,
        wavelength = 16.dp,
        waveSpeed = 38.dp,
    )
}
