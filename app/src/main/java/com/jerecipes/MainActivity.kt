package com.jerecipes

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jerecipes.ui.AuthViewModel
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.screens.*
import com.jerecipes.ui.theme.JerecipesTheme

/**
 * Material predictive-back motion for full-screen surfaces (as in Pixel Settings): easing (.1, .1, 0, 1),
 * exiting surface toward 90% scale, previous surface enters from 110% on pop, with cross-fade.
 * See [Predictive back design](https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back).
 */
private val FullScreenPredictiveEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private const val FULL_SCREEN_PREDICTIVE_MS = 350
private val FullScreenTransformOrigin = TransformOrigin(0.5f, 0.5f)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent {
            JerecipesTheme {
                val authViewModel: AuthViewModel = viewModel()
                val recipeViewModel: RecipeViewModel = viewModel()
                val user by authViewModel.user.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                val navController = rememberNavController()

                var showBottomSheet by remember { mutableStateOf(false) }

                val context = LocalContext.current

                val fullScreenEnter = tween<Float>(FULL_SCREEN_PREDICTIVE_MS, easing = FullScreenPredictiveEasing)
                val fullScreenExit = tween<Float>(FULL_SCREEN_PREDICTIVE_MS, easing = FullScreenPredictiveEasing)

                NavHost(
                    navController = navController,
                    startDestination = if (user == null) "login" else "library",
                    enterTransition = {
                        fadeIn(fullScreenEnter) + scaleIn(
                            initialScale = 0.92f,
                            transformOrigin = FullScreenTransformOrigin,
                            animationSpec = fullScreenEnter
                        )
                    },
                    exitTransition = {
                        fadeOut(fullScreenExit) + scaleOut(
                            targetScale = 0.92f,
                            transformOrigin = FullScreenTransformOrigin,
                            animationSpec = fullScreenExit
                        )
                    },
                    popEnterTransition = {
                        fadeIn(fullScreenEnter) + scaleIn(
                            initialScale = 1.1f,
                            transformOrigin = FullScreenTransformOrigin,
                            animationSpec = fullScreenEnter
                        )
                    },
                    popExitTransition = {
                        fadeOut(fullScreenExit) + scaleOut(
                            targetScale = 0.9f,
                            transformOrigin = FullScreenTransformOrigin,
                            animationSpec = fullScreenExit
                        )
                    }
                ) {
                    composable("login") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LaunchedEffect(Unit) {
                                authViewModel.signInWithGoogle(context)
                            }
                        }
                    }
                    composable("library") {
                        RecipeLibraryScreen(
                            viewModel = recipeViewModel,
                            userDisplayName = user?.displayName,
                            userEmail = user?.email,
                            onRecipeClick = { recipe ->
                                navController.navigate("detail/${recipe.id}")
                            },
                            onAddClick = {
                                showBottomSheet = true
                            },
                            onLogoutClick = {
                                authViewModel.signOut(context)
                            },
                            onPrototypeClick = {
                                navController.navigate("prototype")
                            },
                            onLoadingPrototypeClick = {
                                navController.navigate("loading-showcase")
                            },
                            onColorTokensClick = {
                                navController.navigate("color-tokens-showcase")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable("prototype") {
                        FontShowcaseScreen()
                    }
                    composable("loading-showcase") {
                        LoadingShowcaseScreen()
                    }
                    composable("color-tokens-showcase") {
                        ColorTokensShowcaseScreen()
                    }
                    composable("settings") {
                        SettingsScreen(
                            recipeViewModel = recipeViewModel,
                            onBack = {
                                if (!navController.popBackStack("library", inclusive = false)) {
                                    navController.navigate("library") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        "detail/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId")
                        val listRecipes = recipeViewModel.recipes.collectAsState().value
                        val detailFallback by recipeViewModel.detailFallbackRecipe.collectAsState()
                        val recipe = recipeId?.let { id ->
                            listRecipes.find { it.id == id }
                                ?: detailFallback?.takeIf { it.id == id }
                        }

                        if (recipe != null) {
                            RecipeDetailScreen(
                                recipe = recipe,
                                onBack = {
                                    if (!navController.popBackStack("library", inclusive = false)) {
                                        navController.navigate("library") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onRatingChange = { rating ->
                                    recipeViewModel.updateRating(recipe.id, rating)
                                },
                                viewModel = recipeViewModel,
                                initialScrollOffset = recipeViewModel.pendingScrollOffset.also {
                                    recipeViewModel.pendingScrollOffset = 0
                                }
                            )
                        }
                    }
                }

                if (showBottomSheet) {
                    CreateRecipeBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        onSubmit = { recipe, bitmap ->
                            val result = recipeViewModel.saveRecipe(recipe, bitmap)
                            result.onSuccess { newId ->
                                showBottomSheet = false
                                navController.navigate("detail/$newId")
                            }.onFailure { e ->
                                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        recipeViewModel = recipeViewModel
                    )
                }

                LaunchedEffect(authState) {
                    if (authState is AuthViewModel.AuthState.Error) {
                        Toast.makeText(context, (authState as AuthViewModel.AuthState.Error).message, Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(user) {
                    if (user != null && navController.currentDestination?.route == "login") {
                        navController.navigate("library") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else if (user == null && navController.currentDestination?.route != "login") {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
