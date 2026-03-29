package com.jerecipes.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import java.util.Date

/** Four-tier recipe rating system. Stored as the enum name string in Firestore. */
enum class RecipeRating {
    /** A keeper — would make repeatedly. */
    TOP,
    /** Decent — would make again. */
    GOOD,
    /** Mediocre — edits needed. */
    MID,
    /** Default / not yet rated. */
    NEW
}

/**
 * Data class used for Gemini JSON deserialization.
 * This is the intermediary format parsed from Gemini API responses.
 */
@Serializable
data class GeminiRecipe(
    val title: String = "",
    val ingredients: List<GeminiIngredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val source: String? = null,
    val calories: Int? = null,
    val prepTime: Int? = null,
    val waitTime: Int? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null
)

@Serializable
data class GeminiIngredient(
    val name: String = "",
    val amount: Double? = null,
    val unit: String? = null
)

data class Ingredient(
    val name: String = "",
    val amount: Double? = null,
    val unit: String? = null
)

/**
 * Firestore-backed recipe model. NOT kotlinx.serializable — uses Firestore's
 * own reflection-based mapping via toObjects(). Fields match Firestore document schema.
 */
data class Recipe(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val source: String? = null,
    val images: List<String> = emptyList(),
    val comment: String? = null,
    val calories: Int? = null,
    val prepTime: Int? = null,
    val waitTime: Int? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null,
    /** Stored as the RecipeRating enum name, or null for unrated. */
    val rating: String? = null,
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
) {
    /** Parsed rating enum, falling back to NEW when absent or unrecognised. */
    val parsedRating: RecipeRating
        get() = rating?.let { runCatching { RecipeRating.valueOf(it) }.getOrNull() }
            ?: RecipeRating.NEW

    /** Convert a Gemini-parsed recipe into a Firestore-ready Recipe. */
    companion object {
        fun fromGemini(geminiRecipe: GeminiRecipe): Recipe {
            return Recipe(
                title = geminiRecipe.title,
                ingredients = geminiRecipe.ingredients.map { Ingredient(it.name, it.amount, it.unit) },
                instructions = geminiRecipe.instructions,
                source = geminiRecipe.source,
                calories = geminiRecipe.calories,
                prepTime = geminiRecipe.prepTime,
                waitTime = geminiRecipe.waitTime,
                protein = geminiRecipe.protein,
                carbs = geminiRecipe.carbs,
                fat = geminiRecipe.fat
            )
        }
    }
}
