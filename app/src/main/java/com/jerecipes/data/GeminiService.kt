package com.jerecipes.data

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.jerecipes.BuildConfig
import com.jerecipes.data.model.GeminiRecipe
import com.jerecipes.data.model.Recipe
import kotlinx.serialization.json.Json

class GeminiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val systemPrompt = """
        You are the parsing engine for "Jerecipes", a recipe collection application.
        Your task is to extract a recipe from the provided input (which may be a screenshot, a URL, a blog post, or freeform text).
        Format the output EXACTLY matching the required JSON schema.

        Rules:
        1. "title": Provide a concise, appealing name for the recipe. This can be in the source language or English.
        2. "ingredients" & "instructions": These MUST be translated into English regardless of the input language.
        3. "measurements": All measurements MUST be converted to Metric units (e.g., grams, milliliters, kilograms, liters, degrees Celsius). If the source uses Imperial, convert them to the nearest metric equivalent.
        4. "ingredients": Extract all ingredients. If it is a commodity like "salt to taste", leave "amount" and "unit" fields null.
        5. "instructions": Provide an array of strings for the recipe steps. Break it down into logical steps if it's a blob of text.
        6. "source": Extract the URL if provided, or the contextual source (e.g., "From a photo").
        7. "calories", "prepTime", "waitTime", "protein", "carbs", "fat": These MUST be for the ENTIRE recipe, not per portion/serving. If the source mentions per-serving values, multiply them by the number of servings. Extract these as integers if logically mentioned or if you can estimate them safely from the context. Times should be in minutes. Macros in grams.

        JSON Schema:
        {
          "title": "string",
          "ingredients": [
            { "name": "string", "amount": number, "unit": "string" }
          ],
          "instructions": ["string", "string"],
          "source": "string",
          "calories": number,
          "prepTime": number,
          "waitTime": number,
          "protein": number,
          "carbs": number,
          "fat": number
        }
    """.trimIndent()

    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        generationConfig = com.google.ai.client.generativeai.type.generationConfig {
            responseMimeType = "application/json"
        },
        systemInstruction = content { text(systemPrompt) }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun parseRecipe(input: String, bitmap: Bitmap? = null): Result<Recipe> {
        val prompt = content {
            text(input)
            if (bitmap != null) {
                image(bitmap)
            }
        }

        return try {
            val response = model.generateContent(prompt)
            val jsonString = response.text

            if (!jsonString.isNullOrBlank()) {
                val geminiRecipe = json.decodeFromString<GeminiRecipe>(jsonString)
                Result.success(Recipe.fromGemini(geminiRecipe))
            } else {
                Result.failure(Exception("Model returned empty or non-JSON response"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("API Error: ${e.message}"))
        }
    }

    suspend fun recalculateMetadata(recipe: Recipe): Result<GeminiRecipe> {
        val recipeDetails = """
            Title: ${recipe.title}
            Ingredients:
            ${recipe.ingredients.joinToString("\n") { "- ${it.amount ?: ""} ${it.unit ?: ""} ${it.name}" }}

            Instructions:
            ${recipe.instructions.joinToString("\n")}
        """.trimIndent()

        val prompt = content {
            text("Recalculate nutritional metadata (calories, times, nutrients) for the ENTIRE recipe based on these details:\n\n$recipeDetails")
        }

        return try {
            val response = model.generateContent(prompt)
            val jsonString = response.text

            if (!jsonString.isNullOrBlank()) {
                val geminiRecipe = json.decodeFromString<GeminiRecipe>(jsonString)
                Result.success(geminiRecipe)
            } else {
                Result.failure(Exception("Model returned empty or non-JSON response"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("API Error: ${e.message}"))
        }
    }
}

