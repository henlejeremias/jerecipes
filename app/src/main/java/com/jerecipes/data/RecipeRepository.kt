package com.jerecipes.data

import android.graphics.Bitmap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.jerecipes.data.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class RecipeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val recipesCollection = firestore.collection("recipes")

    fun getRecipes(): Flow<List<Recipe>> {
        val userId = auth.currentUser?.uid ?: ""
        return recipesCollection
            .whereEqualTo("createdBy", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Recipe::class.java)
            }
    }

    suspend fun saveRecipe(recipe: Recipe, bitmap: Bitmap? = null) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User must be logged in to save recipes")
        
        var finalRecipe = recipe.copy(createdBy = userId)
        
        // Handle Image Upload if a new bitmap is provided
        if (bitmap != null) {
            val imageUrl = uploadImage(bitmap, userId)
            finalRecipe = finalRecipe.copy(images = listOf(imageUrl))
        }

        if (finalRecipe.id.isEmpty()) {
            // New recipe
            recipesCollection.add(finalRecipe).await()
        } else {
            // Update existing
            recipesCollection.document(finalRecipe.id).set(finalRecipe).await()
        }
    }

    private suspend fun uploadImage(bitmap: Bitmap, userId: String): String {
        val fileName = "recipes/$userId/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)
        
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()
        
        storageRef.putBytes(data).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun deleteRecipe(recipeId: String) {
        recipesCollection.document(recipeId).delete().await()
    }
}
