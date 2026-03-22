@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.jerecipes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jerecipes.ui.AuthViewModel
import com.jerecipes.ui.RecipeViewModel
import com.jerecipes.ui.screens.CreateRecipeBottomSheet
import com.jerecipes.ui.screens.EditRecipeScreen
import com.jerecipes.ui.screens.LoginScreen
import com.jerecipes.ui.screens.RecipeDetailScreen
import com.jerecipes.ui.screens.RecipeLibraryScreen
import com.jerecipes.ui.theme.JerecipesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JerecipesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val recipeViewModel: RecipeViewModel = viewModel()
                    
                    val user by authViewModel.user.collectAsState()
                    val authState by authViewModel.authState.collectAsState()
                    val isSaving by recipeViewModel.isSaving.collectAsState()
                    var showBottomSheet by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = if (user == null) "login" else "library"
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onSignInClick = {
                                        authViewModel.signInWithGoogle(context)
                                    }
                                )
                            }
                            composable("library") {
                                AnimatedVisibility(visible = true) {
                                    RecipeLibraryScreen(
                                        viewModel = recipeViewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this,
                                        onRecipeClick = { recipe ->
                                            navController.navigate("detail/${recipe.id}")
                                        },
                                        onAddClick = {
                                            showBottomSheet = true
                                        }
                                    )
                                }
                            }
                            composable(
                                "detail/{recipeId}",
                                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val recipeId = backStackEntry.arguments?.getString("recipeId")
                                val recipe = recipeViewModel.recipes.collectAsState().value.find { it.id == recipeId }
                                
                                if (recipe != null) {
                                    AnimatedVisibility(visible = true) {
                                        RecipeDetailScreen(
                                            recipe = recipe,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                            composable("edit") {
                                val pendingRecipe by recipeViewModel.pendingRecipe.collectAsState()
                                val pendingBitmap by recipeViewModel.pendingBitmap.collectAsState()
                                
                                if (pendingRecipe != null) {
                                    EditRecipeScreen(
                                        recipe = pendingRecipe!!,
                                        pendingBitmap = pendingBitmap,
                                        isSaving = isSaving,
                                        onSave = { updatedRecipe ->
                                            recipeViewModel.saveRecipe(updatedRecipe)
                                            navController.navigate("library") {
                                                popUpTo("library") { inclusive = true }
                                            }
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }

                    if (showBottomSheet) {
                        CreateRecipeBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            onRecipeParsed = { recipe, bitmap ->
                                showBottomSheet = false
                                recipeViewModel.setPendingRecipe(recipe, bitmap)
                                navController.navigate("edit")
                            }
                        )
                    }

                    // Handle Auth Errors
                    LaunchedEffect(authState) {
                        if (authState is AuthViewModel.AuthState.Error) {
                            Toast.makeText(context, (authState as AuthViewModel.AuthState.Error).message, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Automatic navigation when user logs in/out
                    LaunchedEffect(user) {
                        if (user != null && navController.currentDestination?.route == "login") {
                            navController.navigate("library") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else if (user == null && navController.currentDestination?.route != "login") {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                }
            }
        }
    }
}
