package com.jerecipes.data

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.jerecipes.data.model.Recipe
import com.jerecipes.data.model.Ingredient
import com.jerecipes.data.model.RecipeRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.util.UUID

class RecipeRepository {
    private val TAG = "RecipeRepository"

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val recipesCollection = firestore.collection("recipes")

    fun getRecipes(): Flow<List<Recipe>> {
        val user = auth.currentUser
        val userId = user?.uid ?: ""
        Log.d(TAG, "Subscribing to recipes for user: $userId")

        return recipesCollection
            .whereEqualTo("createdBy", userId)
            .snapshots()
            .map { snapshot ->
                Log.d(TAG, "Received snapshot with ${snapshot.size()} documents")
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Recipe::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse recipe ${doc.id}, attempting manual fix", e)
                        val data = doc.data ?: return@mapNotNull null

                        val instructionsRaw = data["instructions"]
                        val instructionsList = when (instructionsRaw) {
                            is List<*> -> instructionsRaw.mapNotNull { it?.toString() }
                            is String -> listOf(instructionsRaw)
                            else -> emptyList()
                        }

                        val ingredientsList = (data["ingredients"] as? List<*>)?.mapNotNull {
                            val m = it as? Map<*, *>
                            Ingredient(
                                name = m?.get("name") as? String ?: "",
                                amount = (m?.get("amount") as? Number)?.toDouble(),
                                unit = m?.get("unit") as? String
                            )
                        } ?: emptyList()

                        Recipe(
                            id = doc.id,
                            title = data["title"] as? String ?: "Untitled",
                            ingredients = ingredientsList,
                            instructions = instructionsList,
                            source = data["source"] as? String,
                            images = (data["images"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            comment = data["comment"] as? String,
                            calories = (data["calories"] as? Number)?.toInt(),
                            prepTime = (data["prepTime"] as? Number)?.toInt(),
                            waitTime = (data["waitTime"] as? Number)?.toInt(),
                            protein = (data["protein"] as? Number)?.toInt(),
                            carbs = (data["carbs"] as? Number)?.toInt(),
                            fat = (data["fat"] as? Number)?.toInt(),
                            rating = data["rating"] as? String,
                            createdBy = data["createdBy"] as? String ?: "",
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                        )
                    }
                }.sortedByDescending { it.createdAt ?: java.util.Date(0) }
            }
    }

    suspend fun saveRecipe(recipe: Recipe, bitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User must be logged in to save recipes")

        var finalRecipe = recipe.copy(createdBy = userId)

        if (bitmap != null) {
            try {
                Log.d(TAG, "Starting image upload... userId: $userId")
                val imageUrl = uploadImage(bitmap, userId)
                Log.d(TAG, "Image uploaded successfully. URL: $imageUrl")

                finalRecipe = finalRecipe.copy(images = listOf(imageUrl) + recipe.images)
            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL: Storage upload failed: ${e.message}", e)
            }
        }

        return@withContext if (finalRecipe.id.isEmpty()) {
            Log.d(TAG, "Adding new recipe to Firestore")
            val docRef = recipesCollection.add(finalRecipe).await()
            docRef.id
        } else {
            Log.d(TAG, "Updating existing recipe ${finalRecipe.id} in Firestore")
            recipesCollection.document(finalRecipe.id).set(finalRecipe).await()
            finalRecipe.id
        }
    }

    private suspend fun uploadImage(bitmap: Bitmap, userId: String): String = withContext(Dispatchers.IO) {
        val fileName = "recipes/$userId/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        val scaledBitmap = scaleBitmapIfNeeded(bitmap)

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val data = baos.toByteArray()

        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }

        Log.d(TAG, "Uploading ${data.size} bytes to path: $fileName")
        storageRef.putBytes(data, metadata).await()

        val downloadUrl = storageRef.downloadUrl.await().toString()
        return@withContext downloadUrl
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = 1600
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / Math.max(bitmap.width, bitmap.height)
        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun updateRating(recipeId: String, rating: RecipeRating): Unit = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating rating for $recipeId → $rating")
        recipesCollection.document(recipeId)
            .update("rating", rating.name)
            .await()
    }

    suspend fun deleteImages(imageUrls: List<String>) = withContext(Dispatchers.IO) {
        for (url in imageUrls) {
            try {
                val ref = storage.getReferenceFromUrl(url)
                ref.delete().await()
                Log.d(TAG, "Deleted image from storage: $url")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete image from storage: $url", e)
            }
        }
    }

    suspend fun recalculateMetadata(recipe: Recipe): Result<Recipe> = withContext(Dispatchers.IO) {
        val geminiService = GeminiService()
        val result = geminiService.recalculateMetadata(recipe)

        return@withContext result.map { geminiRecipe ->
            recipe.copy(
                calories = geminiRecipe.calories,
                prepTime = geminiRecipe.prepTime,
                waitTime = geminiRecipe.waitTime,
                protein = geminiRecipe.protein,
                carbs = geminiRecipe.carbs,
                fat = geminiRecipe.fat
            )
        }
    }

    suspend fun deleteRecipe(recipeId: String, imageUrls: List<String> = emptyList()) {
        Log.d(TAG, "Deleting recipe: $recipeId with ${imageUrls.size} images")
        deleteImages(imageUrls)
        recipesCollection.document(recipeId).delete().await()
    }
}
