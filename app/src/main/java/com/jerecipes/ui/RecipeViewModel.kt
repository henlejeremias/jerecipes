package com.jerecipes.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jerecipes.data.RecipeRepository
import com.jerecipes.data.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {
    private val repository = RecipeRepository()

    // Real-time flow from Firestore
    val recipes: StateFlow<List<Recipe>> = repository.getRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State for the recipe currently being parsed/edited before saving
    private val _pendingRecipe = MutableStateFlow<Recipe?>(null)
    val pendingRecipe: StateFlow<Recipe?> = _pendingRecipe.asStateFlow()

    private val _pendingBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingBitmap: StateFlow<Bitmap?> = _pendingBitmap.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun setPendingRecipe(recipe: Recipe, bitmap: Bitmap? = null) {
        _pendingRecipe.value = recipe
        _pendingBitmap.value = bitmap
    }

    fun saveRecipe(recipe: Recipe, bitmap: Bitmap? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Use provided bitmap or the pending one from parsing
                val finalBitmap = bitmap ?: _pendingBitmap.value
                repository.saveRecipe(recipe, finalBitmap)
                
                _pendingRecipe.value = null
                _pendingBitmap.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
