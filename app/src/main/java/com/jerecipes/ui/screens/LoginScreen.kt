@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerecipes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A minimalist Login Page redesigned with Material 3 Expressive principles.
 * 
 * Features exactly one native-style "Log In with Google" button centered in an empty space,
 * following the user's strict requirement for a high-fidelity, uncluttered experience.
 */
@Composable
fun LoginScreen(
    onSignInClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // M3 Expressive Native Log In with Google Button
            // Using a large, tactile Surface for a premium "Expressive" feel.
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSignInClick()
                },
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .height(64.dp), // Expressive height for prominent action
                shape = CircleShape, // Pill-shaped following M3 Expressive FAB patterns
                color = Color.White, // Traditional Google surface for brand recognizability
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Official Google "G" Logo for native branding
                    Icon(
                        painter = painterResource(id = com.jerecipes.R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified // Keep original brand colors
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Text(
                        text = "Log In with Google",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MaterialTheme.typography.titleLarge.fontFamily, // Ensure Expressive Roboto Flex
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 0.sp,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}
