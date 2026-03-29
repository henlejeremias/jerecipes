package com.jerecipes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.*

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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JerecipesTheme {
                val authViewModel: AuthViewModel = viewModel()
                val recipeViewModel: RecipeViewModel = viewModel()
                val user by authViewModel.user.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                
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
                        composable(
                            "library",
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() }
                        ) {
                            RecipeLibraryScreen(
                                viewModel = recipeViewModel,
                                userPhotoUrl = user?.photoUrl?.toString(),
                                userEmail = user?.email,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this,
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
                                }
                            )
                        }
                        composable("prototype") {
                            FontShowcaseScreen(
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
                            arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() }
                        ) { backStackEntry ->
                            val recipeId = backStackEntry.arguments?.getString("recipeId")
                            val recipe = recipeViewModel.recipes.collectAsState().value.find { it.id == recipeId }
                            
                            if (recipe != null) {
                                RecipeDetailScreen(
                                    recipe = recipe,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                    onBack = { 
                                        if (!navController.popBackStack("library", inclusive = false)) {
                                            navController.navigate("library") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    },
                                    onEditClick = {
                                        recipeViewModel.setPendingRecipe(recipe, null)
                                        navController.navigate("edit")
                                    },
                                    onRatingChange = { rating ->
                                        recipeViewModel.updateRating(recipe.id, rating)
                                    },
                                    initialScrollOffset = recipeViewModel.pendingScrollOffset.also {
                                        recipeViewModel.pendingScrollOffset = 0
                                    }
                                )
                            }
                        }
                        composable("edit") {
                            val pendingRecipe by recipeViewModel.pendingRecipe.collectAsState()
                            val pendingBitmap by recipeViewModel.pendingBitmap.collectAsState()
                            val isSaving by recipeViewModel.isSaving.collectAsState()

                            if (pendingRecipe != null) {
                                EditRecipeScreen(
                                    recipe = pendingRecipe!!,
                                    isSaving = isSaving,
                                    onSave = { updatedRecipe, selectedBitmap, imagesToDelete, scrollOffset ->
                                        scope.launch {
                                            recipeViewModel.pendingScrollOffset = scrollOffset
                                            val result = recipeViewModel.saveRecipe(updatedRecipe, selectedBitmap, imagesToDelete)
                                            result.onSuccess { savedId ->
                                                navController.navigate("detail/$savedId") {
                                                    popUpTo("edit") { inclusive = true }
                                                }
                                            }.onFailure { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
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
                            scope.launch {
                                val result = recipeViewModel.saveRecipe(recipe, bitmap)
                                result.onSuccess { newId ->
                                    navController.navigate("detail/$newId")
                                }.onFailure { e ->
                                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onBlankRecipe = {
                            showBottomSheet = false
                            recipeViewModel.setPendingRecipe(
                                com.jerecipes.data.model.Recipe(title = "New Recipe"),
                                null
                            )
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
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
