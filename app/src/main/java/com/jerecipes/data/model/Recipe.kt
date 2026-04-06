package com.jerecipes.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.util.Date

enum class RecipeRating {

    TOP,

    GOOD,

    MID,

    NEW
}

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
    val amount: JsonElement? = null,
    val unit: String? = null
)

data class Ingredient(
    val name: String = "",
    val amount: Double? = null,
    val unit: String? = null
)

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

    val rating: String? = null,
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
) {

    val parsedRating: RecipeRating
        get() = rating?.let { runCatching { RecipeRating.valueOf(it) }.getOrNull() }
            ?: RecipeRating.NEW

    companion object {
        fun fromGemini(geminiRecipe: GeminiRecipe): Recipe {
            return Recipe(
                title = geminiRecipe.title,
                ingredients = geminiRecipe.ingredients.map {
                    Ingredient(it.name, it.amount.parseIngredientAmount(), it.unit)
                },
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

private fun JsonElement?.parseIngredientAmount(): Double? {
    if (this == null || this === JsonNull) return null
    val primitive = this as? JsonPrimitive ?: return null
    return when {
        primitive.isString -> {
            val s = primitive.content.trim()
            if (s.isEmpty()) null else s.toDoubleOrNull()?.takeUnless { it == 0.0 }
        }
        primitive.doubleOrNull != null -> primitive.doubleOrNull!!.takeUnless { it == 0.0 }
        primitive.intOrNull != null -> primitive.intOrNull!!.toDouble().takeUnless { it == 0.0 }
        else -> null
    }
}
