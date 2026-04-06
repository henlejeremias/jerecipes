package com.jerecipes.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.jerecipes.BuildConfig
import com.jerecipes.data.model.GeminiRecipe
import com.jerecipes.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiService(
    customApiKey: String = "",
    customPrompt: String = ""
) {
    private companion object {
        const val TAG = "GeminiService"
        const val TEXT_MODEL_NAME = "gemini-3.1-flash-lite-preview"
        const val IMAGE_MODEL_NAME = "gemini-3.1-flash-image-preview"
        const val IMAGE_ASPECT_RATIO = "4:3"
    }

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
           - "amount" field: Always a string. Use a decimal amount when known (e.g., "200", "1.5"). If no amount is specified (e.g., "salt to taste", "oil for frying"), use an empty string "" for "amount" and put "unit" in "" if needed. NEVER use 0, "0", or numeric zero as a placeholder for missing amounts.
           - No Amount: Ingredients with no amount specified MUST ALWAYS come at the very end of the entire list.
           - Meat Detail: For meat, always specify the part (e.g., "Beef Ribeye"). If not specified in the source, suggest a logical part based on the recipe.
        5. "instructions": Keep the recipe simple and straightforward. Use as few steps as possible while staying clear: merge related actions into one step when sensible. Include only what matters to cook the dish (key times, temperatures, techniques); drop filler, long stories, and redundant detail. Order steps so preparation (chop, wash, measure) still comes before cooking when that order matters.
        6. "source": Extract the URL if provided, or the contextual source (e.g., "From a photo").
        7. "calories", "prepTime", "waitTime", "protein", "carbs", "fat": These MUST be for the ENTIRE recipe, not per portion/serving. If the source mentions per-serving values, multiply them by the number of servings. Extract these as integers if logically mentioned or if you can estimate them safely from the context. Times should be in minutes. Macros in grams.

        JSON Schema:
        {
          "title": "string",
          "ingredients": [
            { "name": "string", "amount": "string", "unit": "string" }
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

        return runJsonRequest(
            operation = "parseRecipe",
            modelName = TEXT_MODEL_NAME,
            prompt = prompt,
            bitmap = bitmap,
            systemPrompt = effectiveSystemPrompt,
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

        return runJsonRequest(
            operation = "editRecipe",
            modelName = TEXT_MODEL_NAME,
            prompt = editPrompt,
            systemPrompt = effectiveSystemPrompt,
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

        return runJsonRequest(
            operation = "recalculateMetadata",
            modelName = TEXT_MODEL_NAME,
            prompt = prompt,
            systemPrompt = effectiveSystemPrompt,
            errorPrefix = "API Error"
        ) { payload ->
            json.decodeFromString<GeminiRecipe>(payload)
        }
    }

    suspend fun generateImage(recipe: Recipe): Result<Bitmap> {
        return try {
            val response = requestContent(
                modelName = IMAGE_MODEL_NAME,
                prompt = buildImagePrompt(recipe),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("IMAGE"),
                    imageConfig = ImageConfig(aspectRatio = IMAGE_ASPECT_RATIO)
                )
            )
            val bitmap = extractImageBitmap(response)
                ?: return Result.failure(Exception(buildNoImageError(response)))
            Result.success(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "generateImage failed", e)
            Result.failure(Exception("Image generation error: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun buildImagePrompt(recipe: Recipe): String {
        val dishName = recipe.title.ifBlank { "Finished plated recipe" }
        val visibleIngredients = recipe.ingredients
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .take(8)
            .joinToString(", ")
            .ifBlank { "Focus on the plated dish instead of raw ingredients." }
        val preparationSteps = recipe.instructions
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(6)
        val preparationCues = if (preparationSteps.isEmpty()) {
            "- No preparation steps were provided."
        } else {
            preparationSteps.joinToString(separator = "\n") { "- $it" }
        }

        return """
            Create a single realistic food photograph for this recipe.

            Recipe title: $dishName
            Key ingredients: $visibleIngredients
            Preparation cues:
            $preparationCues

            Requirements:
            - Show the finished dish only, plated and ready to eat.
            - Reflect the cooking method, texture, doneness, and garnish implied by the preparation cues.
            - Ingredients should appear as part of the final dish, not as raw prep items laid out beside it.
            - Premium cookbook-style food photography, natural lighting, believable portions, accurate colors.
            - Keep the background simple and unobtrusive so the dish stays the clear focus.
            - No people, hands, packaging, cut-off plates, split-screen layout, text overlays, logos, or watermarks.
            - Avoid excessive decoration unless the recipe itself clearly calls for it.
        """.trimIndent()
    }

    private suspend fun <T> runJsonRequest(
        operation: String,
        modelName: String,
        prompt: String,
        bitmap: Bitmap? = null,
        systemPrompt: String? = null,
        errorPrefix: String,
        parser: (String) -> T
    ): Result<T> {
        return try {
            val responseText = requestTextPayload(
                modelName = modelName,
                prompt = prompt,
                bitmap = bitmap,
                systemPrompt = systemPrompt,
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )
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

    private suspend fun requestTextPayload(
        modelName: String,
        prompt: String,
        bitmap: Bitmap? = null,
        systemPrompt: String? = null,
        generationConfig: GenerationConfig? = null
    ): String {
        val response = requestContent(
            modelName = modelName,
            prompt = prompt,
            bitmap = bitmap,
            systemPrompt = systemPrompt,
            generationConfig = generationConfig
        )
        val text = response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.joinToString(separator = "") { it.text.orEmpty() }
            .orEmpty()

        if (text.isBlank()) {
            throw IllegalStateException("Model returned no text payload")
        }

        return text
    }

    private suspend fun requestContent(
        modelName: String,
        prompt: String,
        bitmap: Bitmap? = null,
        systemPrompt: String? = null,
        generationConfig: GenerationConfig? = null
    ): GenerateContentResponse =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                throw IllegalStateException("Gemini API key is missing")
            }

            val requestBody = GenerateContentRequest(
                systemInstruction = systemPrompt
                    ?.takeIf { it.isNotBlank() }
                    ?.let { ApiContent(parts = listOf(ApiPart(text = it))) },
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
                generationConfig = generationConfig
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

                json.decodeFromString(body)
            } finally {
                connection.disconnect()
            }
        }

    private fun extractImageBitmap(response: GenerateContentResponse): Bitmap? {
        response.candidates.forEach { candidate ->
            candidate.content?.parts.orEmpty().forEach { part ->
                val inlineData = part.inlineData ?: return@forEach
                if (!inlineData.mimeType.startsWith("image/")) return@forEach
                val bytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
        return null
    }

    private fun buildNoImageError(response: GenerateContentResponse): String {
        val text = response.candidates
            .flatMap { it.content?.parts.orEmpty() }
            .joinToString(separator = " ") { it.text.orEmpty() }
            .trim()
        return if (text.isNotEmpty()) {
            "Image model returned no image payload. Response: $text"
        } else {
            "Image model returned no image payload"
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
    val generationConfig: GenerationConfig? = null,
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
    val responseMimeType: String? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: ImageConfig? = null
)

@Serializable
private data class ImageConfig(
    val aspectRatio: String? = null
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
