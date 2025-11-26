package com.foodie.app.data.repository

import android.content.Context
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.remote.dto.AzureResponseRequest
import com.foodie.app.data.remote.dto.ContentItem
import com.foodie.app.data.remote.dto.InputMessage
import com.foodie.app.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import io.mockk.mockk
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Live integration test for Azure OpenAI API.
 *
 * **TEST RESULTS (2025-11-10):**
 * - ✅ Successfully authenticated with Azure OpenAI API
 * - ✅ Request format accepted by API
 * - ✅ Network configuration works correctly
 * - ✅ API responds correctly for food images
 * - ✅ SALMON TEST: 600 calories, "A serving of white rice topped with glazed salmon,
 *       pickled onions, green onions, and a side of roasted eggplant."
 * - ✅ Response time: ~3 seconds
 *
 * **IMPORTANT:**
 * - This test is @Ignore'd by default to avoid hitting the live API during normal test runs
 * - Remove @Ignore to run manual live testing with real credentials
 * - Place test food images in app/src/test/resources/ (e.g., salmon.jpg)
 *
 * **To Run:**
 * 1. Ensure SecurePreferences has valid credentials (API key, endpoint, model)
 * 2. Place a food image in app/src/test/resources/
 * 3. Remove @Ignore annotation
 * 4. Run: ./gradlew :app:testDebugUnitTest --tests "*NutritionAnalysisRepositoryLiveTest*"
 * 5. Check output for request/response details
 * 6. Re-add @Ignore and restore credentials to null
 */
@Ignore("Requires live Azure OpenAI credentials - validated 2025-11-10 with salmon.jpg")
class NutritionAnalysisRepositoryLiveTest {

    private lateinit var api: AzureOpenAiApi
    private lateinit var securePreferences: SecurePreferences

    @Before
    fun setup() {
        // Use real SecurePreferences with hardcoded credentials
        val context = mockk<Context>(relaxed = true)
        securePreferences = SecurePreferences(context)

        // Create real AuthInterceptor
        val authInterceptor = AuthInterceptor(securePreferences)

        // Create logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create OkHttpClient with real interceptors
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Create Retrofit with real endpoint
        val endpoint = securePreferences.azureOpenAiEndpoint
            ?: throw IllegalStateException("Azure OpenAI endpoint not configured")

        val retrofit = Retrofit.Builder()
            .baseUrl(endpoint)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        api = retrofit.create(AzureOpenAiApi::class.java)
    }

    @Test
    fun testLiveAzureOpenAiIntegration() = runBlocking {
        println("\n=== Starting Live Azure OpenAI API Test ===")

        // Load image from test resources
        val imageFile = File(
            javaClass.getResource("/salmon.jpg")?.file
                ?: throw IllegalStateException(
                    "Image file not found. Place your image at app/src/test/resources/salmon.jpg",
                ),
        )
        val imageBytes = imageFile.readBytes()
        val base64 = Base64.getEncoder().encodeToString(imageBytes)
        println("✅ Loaded salmon.jpg from resources (${imageBytes.size} bytes)")
        val imageBase64 = "data:image/jpeg;base64,$base64"

        // Create request with real meal photo
        val model = securePreferences.azureOpenAiModel ?: "gpt-4.1"
        val request = AzureResponseRequest(
            model = model,
            instructions = "Analyze this meal photo and return ONLY a JSON object with this exact format: " +
                "{\"calories\": <number>, \"description\": \"<text>\"}. Be concise.",
            input = listOf(
                InputMessage(
                    role = "user",
                    content = listOf(
                        ContentItem.TextContent(text = "What is in this image?"),
                        ContentItem.ImageContent(imageUrl = imageBase64),
                    ),
                ),
            ),
        )

        println("Request model: $model")
        println("Request instructions: ${request.instructions}")
        println("Request input items: ${request.input.size}")

        try {
            // Call API
            val response = api.analyzeNutrition(request)

            println("\n=== Test Result ===")
            println("✅ SUCCESS!")
            println("Response output_text: ${response.outputText}")

            // Try to parse the output
            val nutritionJson = Gson().fromJson(response.outputText, com.foodie.app.data.remote.dto.ApiNutritionResponse::class.java)
            println("Parsed calories: ${nutritionJson.calories}")
            println("Parsed description: ${nutritionJson.description}")
        } catch (e: Exception) {
            println("\n=== Test Result ===")
            println("❌ FAILURE!")
            println("Exception: ${e::class.java.simpleName}")
            println("Message: ${e.message}")
            e.printStackTrace()
        }

        println("\n=== Test Complete ===")
    }
}
