package com.jerecipes.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jerecipes.data.AppSettings
import com.jerecipes.data.GeminiService
import com.jerecipes.data.RecipeLibraryOrderStore
import com.jerecipes.data.RecipeRepository
import com.jerecipes.data.SettingsRepository
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.RecipeRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.Date

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RecipeViewModel"

    val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Returns a GeminiService built from the current settings snapshot. */
    fun currentGeminiService(): GeminiService {
        val s = settings.value
        return GeminiService(
            customApiKey = s.customApiKey,
            customPrompt = s.customPrompt
        )
    }

    private fun buildRepository(): RecipeRepository = RecipeRepository(currentGeminiService())

    private var repository: RecipeRepository = buildRepository()

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

    private var deleteUndoToken = 0L
    private val _pendingDeleteUndo = MutableStateFlow<Pair<Long, Recipe>?>(null)
    /** Non-null while the library should show the post-delete undo snackbar for a recipe snapshot. */
    val pendingDeleteUndo: StateFlow<Pair<Long, Recipe>?> = _pendingDeleteUndo.asStateFlow()

    /**
     * Last recipe shown on the detail route. When Firestore removes the document (e.g. delete),
     * the list no longer contains it but the detail screen must stay up until navigation runs.
     */
    private val _detailFallbackRecipe = MutableStateFlow<Recipe?>(null)
    val detailFallbackRecipe: StateFlow<Recipe?> = _detailFallbackRecipe.asStateFlow()

    fun rememberDetailFallback(recipe: Recipe) {
        _detailFallbackRecipe.value = recipe
    }

    fun clearDetailFallback() {
        _detailFallbackRecipe.value = null
    }

    private val libraryOrderStore = RecipeLibraryOrderStore(application)

    /** Recipes ordered for the library grid (persisted order + new recipes by creation date). */
    val libraryRecipes: StateFlow<List<Recipe>> = combine(
        _recipes,
        libraryOrderStore.orderFlow
    ) { list, order -> recipesWithLibraryOrder(list, order) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    suspend fun saveRecipe(recipe: Recipe, bitmap: Bitmap? = null, imagesToDelete: List<String> = emptyList()): Result<String> {
        _isSaving.value = true
        _error.value = null
        return try {
            val enrichedRecipe = try {
                val recalcResult = repository.recalculateMetadata(recipe)
                recalcResult.getOrDefault(recipe)
            } catch (e: Exception) {
                Log.w(TAG, "Recalculation failed, saving original recipe", e)
                recipe
            }

            val id = repository.saveRecipe(enrichedRecipe, bitmap)

            if (imagesToDelete.isNotEmpty()) {
                try {
                    repository.deleteImages(imagesToDelete)
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical: failed to delete old images", e)
                }
            }

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

    fun offerDeletedRecipeForUndo(recipe: Recipe) {
        deleteUndoToken++
        _pendingDeleteUndo.value = deleteUndoToken to recipe
    }

    fun clearPendingDeleteUndo() {
        _pendingDeleteUndo.value = null
    }

    /** Deletes the recipe document only; storage images are removed after the undo window via [finalizeDeletedRecipeStorage]. */
    suspend fun deleteRecipeDocument(recipeId: String): Result<Unit> {
        return try {
            repository.deleteRecipeDocument(recipeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete document failed", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes on [viewModelScope] so work is not cancelled when the detail composable leaves the tree
     * (Firestore updates the list as soon as the document is gone). [onFinished] is always invoked on the main thread.
     */
    fun deleteRecipeFromDetail(recipe: Recipe, onFinished: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val result = deleteRecipeDocument(recipe.id)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                withContext(Dispatchers.Main.immediate) {
                    onFinished(false, msg)
                }
                return@launch
            }
            offerDeletedRecipeForUndo(recipe)
            withContext(Dispatchers.Main.immediate) {
                onFinished(true, null)
            }
        }
    }

    fun finalizeDeletedRecipeStorage(imageUrls: List<String>) {
        if (imageUrls.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.deleteImages(imageUrls)
            } catch (e: Exception) {
                Log.e(TAG, "Finalize delete storage failed", e)
            }
        }
    }

    /** Re-saves a recipe after undo (document was removed; images were kept in storage). */
    suspend fun restoreRecipeAfterUndo(recipe: Recipe): Result<String> {
        return saveRecipe(recipe, bitmap = null, imagesToDelete = emptyList())
    }

    /**
     * Moves a recipe one step in the library list. [direction] `-1` is toward the top of the list,
     * `+1` toward the bottom. Order is stored locally (DataStore).
     */
    fun moveRecipeInLibrary(recipeId: String, direction: Int) {
        require(direction == -1 || direction == 1)
        viewModelScope.launch {
            val list = _recipes.value
            val order = libraryOrderStore.orderFlow.first()
            val ids = recipesWithLibraryOrder(list, order).map { it.id }.toMutableList()
            val i = ids.indexOf(recipeId)
            if (i < 0) return@launch
            val j = i + direction
            if (j !in ids.indices) return@launch
            val tmp = ids[i]
            ids[i] = ids[j]
            ids[j] = tmp
            libraryOrderStore.saveOrder(ids)
        }
    }

    private fun recipesWithLibraryOrder(recipes: List<Recipe>, savedOrder: List<String>): List<Recipe> {
        val byId = recipes.associateBy { it.id }
        val orderedKnown = savedOrder.mapNotNull { byId[it] }
        val used = savedOrder.filter { it in byId }.toSet()
        val tail = recipes
            .filter { it.id !in used }
            .sortedByDescending { it.createdAt ?: Date(0) }
        return orderedKnown + tail
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
