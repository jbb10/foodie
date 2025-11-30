package com.foodie.app.data.repository

import android.content.Context
import android.net.Uri
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.remote.dto.ApiNutritionResponse
import com.foodie.app.data.remote.dto.AzureResponseRequest
import com.foodie.app.data.remote.dto.ContentItem
import com.foodie.app.data.remote.dto.InputMessage
import com.foodie.app.data.remote.dto.NutritionAnalysisSchema
import com.foodie.app.data.remote.dto.ResponseFormat
import com.foodie.app.data.remote.dto.TextModality
import com.foodie.app.domain.exception.NoFoodDetectedException
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.domain.repository.NutritionAnalysisRepository
import com.foodie.app.util.ImageUtils
import com.foodie.app.util.Result
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import timber.log.Timber

/**
 * Implementation of NutritionAnalysisRepository using Azure OpenAI Responses API.
 *
 * **Workflow:**
 * 1. Encode photo to base64 data URL (via ImageUtils)
 * 2. Build multimodal request with text prompt + image
 * 3. Call Azure OpenAI Responses API
 * 4. Parse JSON response from output_text field
 * 5. Map to NutritionData domain model
 * 6. Handle errors (network, API, parsing)
 *
 * **Error Classification:**
 * - Retryable: IOException (network), HTTP 5xx (server errors)
 * - Non-retryable: HTTP 4xx (client errors), JsonSyntaxException (parse errors)
 *
 * **Model Configuration:**
 * - Model name from SecurePreferences (e.g., "gpt-4.1", "gpt-4-vision-preview")
 * - Falls back to "gpt-4.1" if not configured
 * - System instructions provide consistent JSON output format
 *
 * **Prompt Engineering:**
 * - System instructions: Explain role as nutrition assistant, specify JSON format
 * - User message: Simple analysis request
 * - Image: Base64-encoded JPEG (from PhotoManager via ImageUtils)
 *
 * Reference: Azure OpenAI Responses API
 * https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses
 */
@Singleton
class NutritionAnalysisRepositoryImpl @Inject constructor(
    private val azureOpenAiApi: AzureOpenAiApi,
    private val imageUtils: ImageUtils,
    private val securePreferences: SecurePreferences,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
) : NutritionAnalysisRepository {

    companion object {
        private const val TAG = "NutritionAnalysisRepo"
        private const val DEFAULT_MODEL = "gpt-4.1" // Fallback if not configured
        private const val PROMPT_FILE = "prompts/nutrition_analysis.md"
    }

    /**
     * Loads the system instructions from the assets file.
     * This allows the prompt to be easily edited without recompiling the code.
     */
    private fun loadSystemInstructions(): String {
        return try {
            context.assets.open(PROMPT_FILE).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Failed to load prompt from assets, using fallback")
            // Fallback prompt in case asset file is missing
            """You are a nutrition analysis assistant.
Analyze the image and determine if it contains food.

If NO FOOD is detected (e.g., empty plate, non-food objects, scenery, documents, people, pets, etc.):
Return: {"hasFood": false, "reason": "brief explanation of what was detected instead"}

If FOOD is detected:
Return: {"hasFood": true, "calories": <number>, "description": "<string>"}

Return only the JSON object, no other text."""
        }
    }

    /**
     * Analyzes a meal photo using Azure OpenAI's multimodal vision capabilities.
     *
     * **Process:**
     * 1. Encode photo to base64 data URL
     * 2. Build request with model, instructions, and multimodal input (text + image)
     * 3. Call Azure OpenAI API
     * 4. Parse output_text field as JSON
     * 5. Map to NutritionData domain model
     *
     * **Error Handling:**
     * - Network errors (IOException) → Result.Error
     * - HTTP 4xx client errors → Result.Error (non-retryable, e.g., invalid API key)
     * - HTTP 5xx server errors → Result.Error
     * - JSON parse errors → Result.Error (indicates API format change or prompt issue)
     * - Image encoding errors → Result.Error (indicates corrupted photo)
     *
     * @param photoUri URI pointing to meal photo (from PhotoManager)
     * @return Result.Success with NutritionData, or Result.Error on error
     */
    override suspend fun analyzePhoto(photoUri: Uri): Result<NutritionData> {
        try {
            Timber.tag(TAG).d("Analyzing photo: $photoUri")

            // Step 1: Encode photo to base64 data URL
            val base64DataUrl = try {
                imageUtils.encodeImageToBase64DataUrl(photoUri)
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "IOException encoding image to base64")
                return Result.Error(
                    exception = e,
                    message = "Failed to encode image: ${e.message}",
                )
            }

            if (base64DataUrl == null) {
                Timber.tag(TAG).e("Failed to encode image to base64 (returned null)")
                return Result.Error(
                    exception = IllegalStateException("Image encoding returned null"),
                    message = "Failed to encode image to base64",
                )
            }

            Timber.tag(TAG).d("Encoded image to base64: ${base64DataUrl.length} chars")

            // Step 2: Get model name from preferences (fallback to default)
            val model = securePreferences.azureOpenAiModel ?: DEFAULT_MODEL
            Timber.tag(TAG).d("Using model: $model")

            // Step 3: Build multimodal request with structured outputs
            val request = AzureResponseRequest(
                model = model,
                instructions = loadSystemInstructions(),
                input = listOf(
                    InputMessage(
                        role = "user",
                        content = listOf(
                            ContentItem.TextContent(text = "Analyze this meal and estimate the total calories and macros (protein, carbs, fat)."),
                            ContentItem.ImageContent(imageUrl = base64DataUrl),
                        ),
                    ),
                ),
                text = TextModality(
                    format = ResponseFormat(
                        type = "json_schema",
                        name = "nutrition_analysis_with_macros",
                        strict = true,
                        schema = NutritionAnalysisSchema.schema,
                    ),
                ),
            )

            // Step 4: Call Azure OpenAI API
            Timber.tag(TAG).d("Sending request to Azure OpenAI...")
            val response = try {
                azureOpenAiApi.analyzeNutrition(request)
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "Network error calling Azure OpenAI")
                return Result.Error(
                    exception = e,
                    message = "Network error: ${e.message}",
                )
            } catch (e: HttpException) {
                val statusCode = e.code()
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) {
                    null
                }
                Timber.tag(TAG).e(e, "HTTP error $statusCode from Azure OpenAI. Error body: $errorBody")
                return Result.Error(
                    exception = e,
                    message = "API error ($statusCode): ${e.message}",
                )
            }

            val outputTextFromArray = response.output
                ?.asSequence()
                ?.flatMap { it.content.orEmpty().asSequence() }
                ?.firstOrNull { content ->
                    content.type.equals("output_text", ignoreCase = true) ||
                        content.type.equals("text", ignoreCase = true)
                }
                ?.text

            val outputText = response.outputText?.takeIf { it.isNotBlank() }
                ?: outputTextFromArray?.takeIf { it.isNotBlank() }

            Timber.tag(TAG).d(
                "Received response: status=${response.status}, directLength=${response.outputText?.length}, fallbackLength=${outputTextFromArray?.length}",
            )

            // Step 5: Parse output_text as JSON
            if (outputText.isNullOrBlank()) {
                Timber.tag(TAG).e("Response output_text is null or blank")
                return Result.Error(
                    exception = IllegalStateException("Empty API response"),
                    message = "API returned empty response",
                )
            }

            Timber.tag(TAG).d("Resolved output text: $outputText")

            val apiNutrition = try {
                gson.fromJson(outputText, ApiNutritionResponse::class.java)
            } catch (e: JsonSyntaxException) {
                Timber.tag(TAG).e(e, "JSON parse error parsing output_text: $outputText")
                return Result.Error(
                    exception = e,
                    message = "Failed to parse nutrition data from API response",
                )
            }

            // Step 6: Check if food was detected
            if (apiNutrition.hasFood == false) {
                val reason = apiNutrition.reason?.takeIf { it.isNotBlank() } ?: "No food detected in the image"
                Timber.tag(TAG).w("No food detected: $reason")
                return Result.Error(
                    exception = NoFoodDetectedException(reason),
                    message = reason,
                )
            }

            // Step 7: Map to domain model (validation happens in NutritionData constructor)
            val nutritionData = try {
                NutritionData(
                    calories = apiNutrition.calories ?: throw IllegalArgumentException("Missing calories field"),
                    protein = apiNutrition.protein?.toDouble() ?: throw IllegalArgumentException("Missing protein field"),
                    carbs = apiNutrition.carbs?.toDouble() ?: throw IllegalArgumentException("Missing carbs field"),
                    fat = apiNutrition.fat?.toDouble() ?: throw IllegalArgumentException("Missing fat field"),
                    description = apiNutrition.description?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Missing or empty description field"),
                )
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).e(e, "Validation error creating NutritionData")
                return Result.Error(
                    exception = e,
                    message = "Invalid nutrition data: ${e.message}",
                )
            }

            Timber.tag(TAG).d("Successfully analyzed nutrition: $nutritionData")
            return Result.Success(nutritionData)
        } catch (e: Exception) {
            // Catch-all for unexpected errors
            Timber.tag(TAG).e(e, "Unexpected error analyzing photo")
            return Result.Error(
                exception = e,
                message = "Unexpected error: ${e.message}",
            )
        }
    }
}
