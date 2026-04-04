package com.jerecipes.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockMethod
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.generationConfig as firebaseGenerationConfig
import com.jerecipes.BuildConfig
import com.jerecipes.data.model.GeminiRecipe
import com.jerecipes.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiService(
    private val modelName: String = GeminiModel.FLASH_LITE.id,
    customApiKey: String = "",
    customPrompt: String = ""
) {
    private val TAG = "GeminiService"
    private val apiKey = customApiKey.trim().takeIf { it.isNotEmpty() } ?: BuildConfig.GEMINI_API_KEY

    val systemPrompt = """
        You are the parsing engine for "Jerecipes", a recipe collection application.
        Your task is to extract a recipe from the provided input (which may be a screenshot, a URL, a blog post, or freeform text).
        Format the output EXACTLY matching the required JSON schema.

        Rules:
        1. "title": Provide a maximally short but descriptive name for the recipe. Avoid artistic or flowery titles.
        2. "ingredients" & "instructions": These MUST be translated into English regardless of the input language.
        3. "measurements": All measurements MUST be converted to Metric units. Use short, uniform abbreviations (e.g., "g" instead of "grams", "ml" instead of "milliliters", "kg", "l", "°C"). If the source uses Imperial, convert them to the nearest metric equivalent.
        4. "ingredients": Extract all ingredients and order them as follows:
           - Shopping List Focus: Ingredient names MUST be "what to buy" (e.g., "Avocado") rather than how to prepare them (e.g., "Avocado, pitted and scooped"). Preparation details belong in instructions, not names.
           - Formatting: Avoid using commas or brackets in ingredient names.
           - Cluster by category: 1. Meat, 2. Vegetables, 3. Others, 4. Herbs and Spices.
           - Specificity: Within clusters, specialized ingredients come before generic commodities.
           - Amount: Within categories/specificity levels, ingredients with larger amounts come before those with lesser amounts.
           - No Amount: Ingredients with no amount specified (e.g., "salt to taste") MUST ALWAYS come at the very end of the entire list.
           - Meat Detail: For meat, always specify the part (e.g., "Beef Ribeye"). If not specified in the source, suggest a logical part based on the recipe.
        5. "instructions": Provide an array of strings for the recipe steps. Order them so that all preparational steps (e.g., chopping, washing, measuring) are listed before executional steps (e.g., heating the pan, frying).
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

    private val effectiveSystemPrompt = if (customPrompt.isBlank()) {
        systemPrompt
    } else {
        "$systemPrompt\n\nAdditional instructions from user:\n${customPrompt.trim()}"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun parseRecipe(input: String, bitmap: Bitmap? = null): Result<Recipe> {
        val prompt = input.ifBlank {
            "Extract a recipe from the provided image and return only valid JSON."
        }

        return runGeminiRequest(
            operation = "parseRecipe",
            prompt = prompt,
            bitmap = bitmap,
            errorPrefix = "API Error"
        ) { payload ->
            Recipe.fromGemini(json.decodeFromString<GeminiRecipe>(payload))
        }
    }

    suspend fun editRecipe(recipe: Recipe, prompt: String): Result<Recipe> {
        val recipeContext = buildString {
            appendLine("Title: ${recipe.title}")
            recipe.comment?.let { appendLine("Comment: $it") }
            appendLine("Ingredients:")
            recipe.ingredients.forEach {
                appendLine("- ${it.amount ?: ""} ${it.unit ?: ""} ${it.name}".trim())
            }
            appendLine("Instructions:")
            recipe.instructions.forEachIndexed { i, step -> appendLine("${i + 1}. $step") }
            recipe.calories?.let { appendLine("Calories: $it kcal") }
            recipe.prepTime?.let { appendLine("Prep Time: $it min") }
            recipe.waitTime?.let { appendLine("Wait Time: $it min") }
            recipe.protein?.let { appendLine("Protein: ${it}g") }
            recipe.carbs?.let { appendLine("Carbs: ${it}g") }
            recipe.fat?.let { appendLine("Fat: ${it}g") }
            recipe.source?.let { appendLine("Source: $it") }
        }

        val editPrompt = """
            Here is an existing recipe:

            $recipeContext

            The user wants to make this change: "$prompt"

            Apply the requested change and return the FULL updated recipe in the same JSON schema.
            Keep everything that was not changed intact. Recalculate nutritional values if the change affects them.
        """.trimIndent()

        return runGeminiRequest(
            operation = "editRecipe",
            prompt = editPrompt,
            errorPrefix = "Edit failed"
        ) { payload ->
            val edited = Recipe.fromGemini(json.decodeFromString<GeminiRecipe>(payload))
            edited.copy(
                id = recipe.id,
                images = recipe.images,
                comment = recipe.comment,
                rating = recipe.rating,
                createdBy = recipe.createdBy,
                createdAt = recipe.createdAt
            )
        }
    }

    suspend fun recalculateMetadata(recipe: Recipe): Result<GeminiRecipe> {
        val details = """
            Title: ${recipe.title}
            Ingredients:
            ${recipe.ingredients.joinToString("\n") { "- ${it.amount ?: ""} ${it.unit ?: ""} ${it.name}".trim() }}

            Instructions:
            ${recipe.instructions.joinToString("\n")}
        """.trimIndent()

        val prompt = """
            Recalculate nutritional metadata (calories, times, nutrients) for the ENTIRE recipe based on these details.
            Return only valid JSON in the existing schema.

            $details
        """.trimIndent()

        return runGeminiRequest(
            operation = "recalculateMetadata",
            prompt = prompt,
            errorPrefix = "API Error"
        ) { payload ->
            json.decodeFromString<GeminiRecipe>(payload)
        }
    }

    // Image generation stays on Firebase AI SDK, which is already part of the app.
    suspend fun generateImage(recipe: Recipe): Result<Bitmap> {
        val imagePrompt = """
            Photorealistic photo of: ${recipe.title}.
            Main ingredients visible: ${recipe.ingredients.take(5).joinToString { it.name }}.
            Neutral, clean food photography. No excessive decoration.
        """.trimIndent()

        return try {
            val safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.NONE, HarmBlockMethod.SEVERITY),
                SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.NONE, HarmBlockMethod.SEVERITY),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.NONE, HarmBlockMethod.SEVERITY),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.NONE, HarmBlockMethod.SEVERITY),
            )
            val imageModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-image-preview",
                generationConfig = firebaseGenerationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE, ResponseModality.TEXT)
                },
                safetySettings = safetySettings
            )

            val response = imageModel.generateContent(
                com.google.firebase.ai.type.content { text(imagePrompt) }
            )

            var bitmap: Bitmap? = null
            response.candidates.firstOrNull()?.content?.parts?.forEach { part ->
                when (part) {
                    is ImagePart -> {
                        bitmap = part.image
                        Log.d(TAG, "Got ImagePart")
                    }
                    is InlineDataPart -> {
                        bitmap = BitmapFactory.decodeByteArray(part.inlineData, 0, part.inlineData.size)
                        Log.d(TAG, "Got InlineDataPart (${part.inlineData.size} bytes)")
                    }
                }
            }

            if (bitmap != null) {
                Result.success(bitmap!!)
            } else {
                Result.failure(Exception("Image model returned no image part"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateImage failed", e)
            Result.failure(Exception("Image generation error: ${e.localizedMessage ?: e.message}"))
        }
    }

    private suspend fun <T> runGeminiRequest(
        operation: String,
        prompt: String,
        bitmap: Bitmap? = null,
        errorPrefix: String,
        parser: (String) -> T
    ): Result<T> {
        return try {
            val responseText = requestJsonResponse(prompt = prompt, bitmap = bitmap)
            val payload = stripMarkdown(responseText)
            if (payload.isBlank()) {
                Result.failure(Exception("Model returned empty response"))
            } else {
                Result.success(parser(payload))
            }
        } catch (e: Exception) {
            Log.e(TAG, "$operation failed", e)
            Result.failure(Exception("$errorPrefix: ${e.localizedMessage ?: e.message}"))
        }
    }

    private suspend fun requestJsonResponse(prompt: String, bitmap: Bitmap? = null): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                throw IllegalStateException("Gemini API key is missing")
            }

            val requestBody = GenerateContentRequest(
                systemInstruction = ApiContent(parts = listOf(ApiPart(text = effectiveSystemPrompt))),
                contents = listOf(
                    ApiContent(
                        parts = buildList {
                            add(ApiPart(text = prompt))
                            if (bitmap != null) {
                                add(
                                    ApiPart(
                                        inlineData = InlineData(
                                            mimeType = "image/jpeg",
                                            data = bitmap.toBase64Jpeg()
                                        )
                                    )
                                )
                            }
                        }
                    )
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            try {
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(json.encodeToString(requestBody))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    throw IllegalStateException(parseApiError(body).ifBlank { "HTTP $code" })
                }

                val response = json.decodeFromString<GenerateContentResponse>(body)
                val text = response.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    ?.joinToString(separator = "") { it.text.orEmpty() }
                    .orEmpty()

                if (text.isBlank()) {
                    throw IllegalStateException("Model returned no text payload")
                }

                text
            } finally {
                connection.disconnect()
            }
        }

    private fun parseApiError(rawBody: String): String {
        return runCatching {
            json.decodeFromString<ErrorEnvelope>(rawBody).error.message
        }.getOrElse {
            rawBody.ifBlank { "Unknown Gemini API error" }
        }
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val bytes = ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun stripMarkdown(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
        return s
    }
}

@Serializable
private data class GenerateContentRequest(
    val contents: List<ApiContent>,
    val generationConfig: GenerationConfig,
    val systemInstruction: ApiContent? = null
)

@Serializable
private data class ApiContent(
    val parts: List<ApiPart>
)

@Serializable
private data class ApiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
private data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class GenerationConfig(
    val responseMimeType: String
)

@Serializable
private data class GenerateContentResponse(
    val candidates: List<ApiCandidate> = emptyList()
)

@Serializable
private data class ApiCandidate(
    val content: ApiContent? = null
)

@Serializable
private data class ErrorEnvelope(
    val error: ApiError
)

@Serializable
private data class ApiError(
    val message: String = ""
)
