package com.jerecipes.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jerecipes.data.AppSettings
import com.jerecipes.data.GeminiService
import com.jerecipes.data.RecipeRepository
import com.jerecipes.data.SettingsRepository
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RecipeViewModel"

    val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Returns a GeminiService built from the current settings snapshot. */
    fun currentGeminiService(): GeminiService {
        val s = settings.value
        return GeminiService(
            modelName = s.modelName,
            customApiKey = s.customApiKey,
            customPrompt = s.customPrompt
        )
    }

    private fun buildRepository(): RecipeRepository = RecipeRepository(currentGeminiService())

    private var repository: RecipeRepository = buildRepository()

    private val _pendingRecipe = MutableStateFlow<Recipe?>(null)
    val pendingRecipe: StateFlow<Recipe?> = _pendingRecipe.asStateFlow()

    private val _pendingBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingBitmap: StateFlow<Bitmap?> = _pendingBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    var pendingScrollOffset: Int = 0

    private var recipesJob: Job? = null
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    init {
        // Rebuild repository whenever settings change so new API calls use updated config
        viewModelScope.launch {
            settings.collect {
                repository = buildRepository()
            }
        }

        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener {
            refreshRecipes()
        }
    }

    private fun refreshRecipes() {
        recipesJob?.cancel()
        recipesJob = viewModelScope.launch {
            _isLoading.value = true
            repository.getRecipes()
                .catch { e ->
                    Log.e(TAG, "Error in recipes flow", e)
                    _error.value = "Failed to load recipes: ${e.message}"
                    _isLoading.value = false
                }
                .collect { list ->
                    Log.d(TAG, "Recipes flow emitted ${list.size} items")
                    _recipes.value = list
                    _isLoading.value = false
                }
        }
    }

    fun setPendingRecipe(recipe: Recipe, bitmap: Bitmap? = null) {
        _pendingRecipe.value = recipe
        _pendingBitmap.value = bitmap
    }

    suspend fun saveRecipe(recipe: Recipe, bitmap: Bitmap? = null, imagesToDelete: List<String> = emptyList()): Result<String> {
        _isSaving.value = true
        _error.value = null
        return try {
            val finalBitmap = bitmap ?: _pendingBitmap.value

            val enrichedRecipe = try {
                val recalcResult = repository.recalculateMetadata(recipe)
                recalcResult.getOrDefault(recipe)
            } catch (e: Exception) {
                Log.w(TAG, "Recalculation failed, saving original recipe", e)
                recipe
            }

            val id = repository.saveRecipe(enrichedRecipe, finalBitmap)

            if (imagesToDelete.isNotEmpty()) {
                try {
                    repository.deleteImages(imagesToDelete)
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical: failed to delete old images", e)
                }
            }

            _pendingRecipe.value = null
            _pendingBitmap.value = null
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Save recipe failed", e)
            val msg = e.message ?: "Unknown error"
            _error.value = msg
            Result.failure(e)
        } finally {
            _isSaving.value = false
        }
    }

    fun deleteRecipe(recipeId: String, imageUrls: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId, imageUrls)
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
            }
        }
    }

    fun updateRating(recipeId: String, rating: RecipeRating) {
        viewModelScope.launch {
            try {
                repository.updateRating(recipeId, rating)
            } catch (e: Exception) {
                Log.e(TAG, "Update rating failed", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun editRecipeWithPrompt(recipe: Recipe, prompt: String): Result<String> {
        _isEditing.value = true
        _error.value = null
        return try {
            val gemini = currentGeminiService()
            val editResult = gemini.editRecipe(recipe, prompt)
            val editedRecipe = editResult.getOrThrow()
            val savedId = repository.saveRecipe(editedRecipe)
            Result.success(savedId)
        } catch (e: Exception) {
            Log.e(TAG, "NL edit failed", e)
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isEditing.value = false
        }
    }
}
