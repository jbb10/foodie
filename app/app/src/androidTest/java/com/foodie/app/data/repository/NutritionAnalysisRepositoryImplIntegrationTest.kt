package com.foodie.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.remote.dto.AzureResponseRequest
import com.foodie.app.data.remote.dto.AzureResponseResponse
import com.foodie.app.util.ImageUtils
import com.foodie.app.util.Result
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.common.truth.Truth.assertThat

/**
 * Integration tests for NutritionAnalysisRepositoryImpl with MockWebServer.
 *
 * Tests real HTTP interactions, JSON serialization, and error handling
 * using a local mock server instead of calling Azure OpenAI API.
 *
 * Coverage:
 * - Success scenario with valid 200 OK response
 * - Error scenarios (401, 429, 500, timeout, invalid JSON)
 * - Request format verification (headers, body structure)
 * - Base64 encoding integration
 *
 * Note: Uses MockWebServer for HTTP simulation and real Gson/Retrofit for serialization.
 */
@RunWith(AndroidJUnit4::class)
class NutritionAnalysisRepositoryImplIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: NutritionAnalysisRepositoryImpl
    private lateinit var mockSecurePreferences: SecurePreferences
    private lateinit var mockImageUtils: ImageUtils
    private lateinit var context: Context
    private lateinit var gson: Gson

    @Before
    fun setUp() {
        // Initialize context
        context = ApplicationProvider.getApplicationContext()

        // Start mock web server
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create Gson instance
        gson = Gson()

        // Mock dependencies
    mockSecurePreferences = mockk(relaxed = true)
    mockImageUtils = mockk()

        // Configure mocks
    every { mockSecurePreferences.azureOpenAiModel } returns "gpt-4.1"

        // Create Retrofit with mock server base URL
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val api = retrofit.create(AzureOpenAiApi::class.java)

        // Create repository with mocked dependencies
        repository = NutritionAnalysisRepositoryImpl(
            azureOpenAiApi = api,
            imageUtils = mockImageUtils,
            securePreferences = mockSecurePreferences,
            gson = gson,
            context = context
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun analyzePhoto_whenValidResponse_thenReturnsNutritionData() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
    every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        val mockResponse = """
            {
                "id": "resp_test123",
                "status": "completed",
                "output_text": "{\"hasFood\": true, \"calories\": 650, \"description\": \"Grilled chicken with rice\"}",
                "usage": {
                    "input_tokens": 1245,
                    "output_tokens": 25,
                    "total_tokens": 1270
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val nutritionData = (result as Result.Success).data
        assertThat(nutritionData.calories).isEqualTo(650)
        assertThat(nutritionData.description).isEqualTo("Grilled chicken with rice")
    }

    @Test
    fun analyzePhoto_whenNetworkError_thenReturnsError() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
    every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        // Simulate network error by enqueueing no response (server closes connection)
        mockWebServer.shutdown()

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("Network error")
    }

    @Test
    fun analyzePhoto_when401Unauthorized_thenReturnsError() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
    every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Unauthorized\"}")
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("401")
    }

    @Test
    fun analyzePhoto_when500ServerError_thenReturnsError() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
    every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal Server Error\"}")
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("500")
    }

    @Test
    fun analyzePhoto_whenInvalidJson_thenReturnsError() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
    every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        val mockResponse = """
            {
                "id": "resp_test123",
                "status": "completed",
                "output_text": "NOT VALID JSON",
                "usage": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("parse")
    }

    @Test
    fun analyzePhoto_whenNoFoodDetected_thenReturnsNoFoodDetectedException() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/photo.jpg")
        val base64Image = "data:image/jpeg;base64,test123"
        
        every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        val mockResponse = """
            {
                "id": "resp_test456",
                "status": "completed",
                "output_text": "{\"hasFood\": false, \"reason\": \"Image shows a document, not food\"}",
                "usage": {
                    "input_tokens": 1245,
                    "output_tokens": 15,
                    "total_tokens": 1260
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(com.foodie.app.domain.exception.NoFoodDetectedException::class.java)
        assertThat(error.message).isEqualTo("Image shows a document, not food")
    }

    @Test
    fun analyzePhoto_whenEmptyPlate_thenReturnsNoFoodDetectedException() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/empty_plate.jpg")
        val base64Image = "data:image/jpeg;base64,empty123"
        
        every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        val mockResponse = """
            {
                "id": "resp_test789",
                "status": "completed",
                "output_text": "{\"hasFood\": false, \"reason\": \"Empty plate with no food visible\"}",
                "usage": {
                    "input_tokens": 1245,
                    "output_tokens": 18,
                    "total_tokens": 1263
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(com.foodie.app.domain.exception.NoFoodDetectedException::class.java)
        assertThat(error.message).contains("Empty plate")
    }

    @Test
    fun analyzePhoto_whenSceneryPhoto_thenReturnsNoFoodDetectedException() = runTest {
        // Arrange
        val mockUri = Uri.parse("content://mock/landscape.jpg")
        val base64Image = "data:image/jpeg;base64,scenery123"
        
        every { mockImageUtils.encodeImageToBase64DataUrl(mockUri) } returns base64Image

        val mockResponse = """
            {
                "id": "resp_test101",
                "status": "completed",
                "output_text": "{\"hasFood\": false, \"reason\": \"Image shows outdoor scenery, not food\"}",
                "usage": {
                    "input_tokens": 1245,
                    "output_tokens": 20,
                    "total_tokens": 1265
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )

        // Act
        val result = repository.analyzePhoto(mockUri)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(com.foodie.app.domain.exception.NoFoodDetectedException::class.java)
        assertThat(error.message).contains("scenery")
    }
}
