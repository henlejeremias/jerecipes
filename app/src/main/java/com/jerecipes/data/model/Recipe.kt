package com.jerecipes.data.model

import kotlinx.serialization.Serializable

/**
 * Data class used for Gemini JSON deserialization.
 * This is the intermediary format parsed from Gemini API responses.
 */
@Serializable
data class GeminiRecipe(
    val title: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: String = "",
    val source: String? = null,
    val calories: Int? = null
)

@Serializable
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
    val id: String = "",
    val title: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: String = "",
    val source: String? = null,
    val images: List<String> = emptyList(),
    val comment: String? = null,
    val calories: Int? = null,
    val createdBy: String = ""
) {
    /** Convert a Gemini-parsed recipe into a Firestore-ready Recipe. */
    companion object {
        fun fromGemini(geminiRecipe: GeminiRecipe): Recipe {
            return Recipe(
                title = geminiRecipe.title,
                ingredients = geminiRecipe.ingredients,
                instructions = geminiRecipe.instructions,
                source = geminiRecipe.source,
                calories = geminiRecipe.calories
            )
        }
    }
}
