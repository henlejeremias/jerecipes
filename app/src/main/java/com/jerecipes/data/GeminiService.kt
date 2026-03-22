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
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey
    )

    private val systemPrompt = """
        You are the parsing engine for "Jerecipes", a recipe collection application.
        Your task is to extract a recipe from the provided input (which may be a screenshot, a URL, a blog post, or freeform text). 
        Format the output EXACTLY matching the required JSON schema. 

        Rules:
        1. "title": Provide a concise, appealing name for the recipe. This can be in the source language or English.
        2. "ingredients" & "instructions": These MUST be translated into English regardless of the input language.
        3. "measurements": All measurements MUST be converted to Metric units (e.g., grams, milliliters, kilograms, liters, degrees Celsius). If the source uses Imperial (cups, ounces, Fahrenheit), convert them to the nearest metric equivalent.
        4. "ingredients": Extract all ingredients. If it is a commodity like "salt to taste", leave "amount" and "unit" fields null. 
        5. "instructions": Provide a clean, sequentially numbered string for the recipe steps.
        6. "source": Extract the URL if provided, or the contextual source (e.g., "From a photo").
        7. "calories": If mentioned, extract as an integer.

        JSON Schema:
        {
          "title": "string",
          "ingredients": [
            { "name": "string", "amount": number, "unit": "string" }
          ],
          "instructions": "string",
          "source": "string",
          "calories": number
        }
        
        Output ONLY the raw JSON string.
    """.trimIndent()

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

    suspend fun parseRecipe(input: String, bitmap: Bitmap? = null): Recipe? {
        val prompt = content {
            text(systemPrompt)
            text(input)
            if (bitmap != null) {
                image(bitmap)
            }
        }

        return try {
            val response = model.generateContent(prompt)
            val jsonString = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim()
            
            if (jsonString != null) {
                val geminiRecipe = json.decodeFromString<GeminiRecipe>(jsonString)
                Recipe.fromGemini(geminiRecipe)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
