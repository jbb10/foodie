package com.foodie.app.domain.error

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorHandler utility.
 *
 * Tests error classification, user messaging, retry logic, and performance requirements.
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
class ErrorHandlerTest {
    
    private lateinit var errorHandler: ErrorHandler
    
    @Before
    fun setup() {
        errorHandler = ErrorHandler()
    }
    
    // ========================================
    // AC #3: Error Classification Tests
    // ========================================
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: IOException → NetworkError
     */
    @Test
    fun `classify should map IOException to NetworkError`() {
        // Given: IOException
        val exception = IOException("Network unreachable")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as NetworkError
        assertThat(errorType).isInstanceOf(ErrorType.NetworkError::class.java)
        assertThat((errorType as ErrorType.NetworkError).cause).isEqualTo(exception)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: SocketTimeoutException → NetworkError
     */
    @Test
    fun `classify should map SocketTimeoutException to NetworkError`() {
        // Given: SocketTimeoutException
        val exception = SocketTimeoutException("Connection timed out")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as NetworkError
        assertThat(errorType).isInstanceOf(ErrorType.NetworkError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: UnknownHostException → NetworkError
     */
    @Test
    fun `classify should map UnknownHostException to NetworkError`() {
        // Given: UnknownHostException
        val exception = UnknownHostException("api.openai.azure.com")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as NetworkError
        assertThat(errorType).isInstanceOf(ErrorType.NetworkError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: HttpException 500 → ServerError
     */
    @Test
    fun `classify should map HttpException 500 to ServerError`() {
        // Given: HttpException with status code 500
        val response = Response.error<Any>(
            500,
            "Internal Server Error".toResponseBody("text/plain".toMediaType())
        )
        val exception = HttpException(response)
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as ServerError with correct status code
        assertThat(errorType).isInstanceOf(ErrorType.ServerError::class.java)
        assertThat((errorType as ErrorType.ServerError).statusCode).isEqualTo(500)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: HttpException 503 → ServerError
     */
    @Test
    fun `classify should map HttpException 503 to ServerError`() {
        // Given: HttpException with status code 503
        val response = Response.error<Any>(
            503,
            "Service Unavailable".toResponseBody("text/plain".toMediaType())
        )
        val exception = HttpException(response)
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as ServerError
        assertThat(errorType).isInstanceOf(ErrorType.ServerError::class.java)
        assertThat((errorType as ErrorType.ServerError).statusCode).isEqualTo(503)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: HttpException 401 → AuthError
     */
    @Test
    fun `classify should map HttpException 401 to AuthError`() {
        // Given: HttpException with status code 401
        val response = Response.error<Any>(
            401,
            "Unauthorized".toResponseBody("text/plain".toMediaType())
        )
        val exception = HttpException(response)
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as AuthError
        assertThat(errorType).isInstanceOf(ErrorType.AuthError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: HttpException 403 → AuthError
     */
    @Test
    fun `classify should map HttpException 403 to AuthError`() {
        // Given: HttpException with status code 403
        val response = Response.error<Any>(
            403,
            "Forbidden".toResponseBody("text/plain".toMediaType())
        )
        val exception = HttpException(response)
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as AuthError
        assertThat(errorType).isInstanceOf(ErrorType.AuthError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: HttpException 429 → RateLimitError
     */
    @Test
    fun `classify should map HttpException 429 to RateLimitError`() {
        // Given: HttpException with status code 429
        val response = Response.error<Any>(
            429,
            "Too Many Requests".toResponseBody("text/plain".toMediaType())
        )
        val exception = HttpException(response)
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as RateLimitError
        assertThat(errorType).isInstanceOf(ErrorType.RateLimitError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: JsonSyntaxException → ParseError
     */
    @Test
    fun `classify should map JsonSyntaxException to ParseError`() {
        // Given: JsonSyntaxException
        val exception = JsonSyntaxException("Expected BEGIN_OBJECT but was STRING")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as ParseError
        assertThat(errorType).isInstanceOf(ErrorType.ParseError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: JsonParseException → ParseError
     */
    @Test
    fun `classify should map JsonParseException to ParseError`() {
        // Given: JsonParseException
        val exception = JsonParseException("Malformed JSON")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as ParseError
        assertThat(errorType).isInstanceOf(ErrorType.ParseError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: IllegalArgumentException with validation context → ValidationError
     */
    @Test
    fun `classify should map IllegalArgumentException with validation context to ValidationError`() {
        // Given: IllegalArgumentException with validation message
        val exception = IllegalArgumentException("Invalid calories: must be between 1 and 5000")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as ValidationError with parsed field and reason
        assertThat(errorType).isInstanceOf(ErrorType.ValidationError::class.java)
        val validationError = errorType as ErrorType.ValidationError
        assertThat(validationError.field).isEqualTo("calories")
        assertThat(validationError.reason).isEqualTo("must be between 1 and 5000")
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: SecurityException → PermissionDenied
     */
    @Test
    fun `classify should map SecurityException to PermissionDenied`() {
        // Given: SecurityException
        val exception = SecurityException("Health Connect permission denied")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as PermissionDenied
        assertThat(errorType).isInstanceOf(ErrorType.PermissionDenied::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: Unknown exception → UnknownError
     */
    @Test
    fun `classify should map unknown exception to UnknownError`() {
        // Given: Generic RuntimeException
        val exception = RuntimeException("Unexpected error")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as UnknownError
        assertThat(errorType).isInstanceOf(ErrorType.UnknownError::class.java)
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies all exception types
     * Test: IllegalArgumentException without validation context → UnknownError
     */
    @Test
    fun `classify should map IllegalArgumentException without validation context to UnknownError`() {
        // Given: IllegalArgumentException without "Invalid field:" pattern
        val exception = IllegalArgumentException("Something went wrong")
        
        // When: Exception is classified
        val errorType = errorHandler.classify(exception)
        
        // Then: Classified as UnknownError (no validation context)
        assertThat(errorType).isInstanceOf(ErrorType.UnknownError::class.java)
    }
    
    // ========================================
    // AC #4: User Message Tests
    // ========================================
    
    /**
     * AC #4: ErrorHandler provides user-friendly error messages for each error type
     * AC #6: All error types map to actionable user guidance
     * Test: All error types return user-friendly messages
     */
    @Test
    fun `getUserMessage should return user-friendly messages for all error types`() {
        // Test each error type has a user-friendly message
        val testCases = mapOf(
            ErrorType.NetworkError(IOException()) to "Request timed out. Check your internet connection.",
            ErrorType.ServerError(500, "Internal Server Error") to "Service temporarily unavailable. Will retry automatically.",
            ErrorType.HealthConnectUnavailable to "Health Connect required. Install from Play Store?",
            ErrorType.AuthError("Unauthorized") to "API key invalid. Check settings.",
            ErrorType.RateLimitError(null) to "Too many requests. Please wait a moment.",
            ErrorType.RateLimitError(60) to "Too many requests. Please wait 60 seconds.",
            ErrorType.ParseError(JsonSyntaxException("")) to "Unexpected response from AI service.",
            ErrorType.ValidationError("calories", "must be between 1 and 5000") to "Invalid calories: must be between 1 and 5000",
            ErrorType.PermissionDenied(listOf("Health Connect")) to "Health Connect permissions required. Tap to grant access.",
            ErrorType.UnknownError(RuntimeException()) to "An unexpected error occurred. Please try again."
        )
        
        testCases.forEach { (errorType, expectedMessage) ->
            val message = errorHandler.getUserMessage(errorType)
            assertThat(message).isEqualTo(expectedMessage)
        }
    }
    
    /**
     * AC #4: ErrorHandler provides user-friendly error messages
     * Test: Error messages are non-technical (no jargon)
     */
    @Test
    fun `getUserMessage should not contain technical jargon`() {
        // Given: Various error types
        val errorTypes = listOf(
            ErrorType.NetworkError(IOException()),
            ErrorType.ServerError(500, "Internal Server Error"),
            ErrorType.AuthError("Unauthorized"),
            ErrorType.ParseError(JsonSyntaxException(""))
        )
        
        // When: User messages generated
        val messages = errorTypes.map { errorHandler.getUserMessage(it) }
        
        // Then: No technical terms (exception, stack trace, JSON, HTTP)
        val technicalTerms = listOf("exception", "stack", "trace", "http", "json", "syntax")
        messages.forEach { message ->
            technicalTerms.forEach { term ->
                assertThat(message.lowercase()).doesNotContain(term)
            }
        }
    }
    
    // ========================================
    // AC #3: Retry Logic Tests
    // ========================================
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: NetworkError is retryable
     */
    @Test
    fun `isRetryable should return true for NetworkError`() {
        // Given: NetworkError
        val error = ErrorType.NetworkError(IOException())
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns true
        assertThat(retryable).isTrue()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: ServerError is retryable
     */
    @Test
    fun `isRetryable should return true for ServerError`() {
        // Given: ServerError
        val error = ErrorType.ServerError(503, "Service Unavailable")
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns true
        assertThat(retryable).isTrue()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: HealthConnectUnavailable is retryable
     */
    @Test
    fun `isRetryable should return true for HealthConnectUnavailable`() {
        // Given: HealthConnectUnavailable
        val error = ErrorType.HealthConnectUnavailable
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns true
        assertThat(retryable).isTrue()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: AuthError is not retryable
     */
    @Test
    fun `isRetryable should return false for AuthError`() {
        // Given: AuthError
        val error = ErrorType.AuthError("Unauthorized")
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false
        assertThat(retryable).isFalse()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: RateLimitError is not retryable
     */
    @Test
    fun `isRetryable should return false for RateLimitError`() {
        // Given: RateLimitError
        val error = ErrorType.RateLimitError(60)
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false (manual retry required, not automatic)
        assertThat(retryable).isFalse()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: ParseError is not retryable
     */
    @Test
    fun `isRetryable should return false for ParseError`() {
        // Given: ParseError
        val error = ErrorType.ParseError(JsonSyntaxException(""))
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false
        assertThat(retryable).isFalse()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: ValidationError is not retryable
     */
    @Test
    fun `isRetryable should return false for ValidationError`() {
        // Given: ValidationError
        val error = ErrorType.ValidationError("calories", "must be between 1 and 5000")
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false
        assertThat(retryable).isFalse()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: PermissionDenied is not retryable
     */
    @Test
    fun `isRetryable should return false for PermissionDenied`() {
        // Given: PermissionDenied
        val error = ErrorType.PermissionDenied(listOf("Health Connect"))
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false
        assertThat(retryable).isFalse()
    }
    
    /**
     * AC #3: ErrorHandler correctly classifies retryable vs non-retryable errors
     * Test: UnknownError is not retryable
     */
    @Test
    fun `isRetryable should return false for UnknownError`() {
        // Given: UnknownError
        val error = ErrorType.UnknownError(RuntimeException())
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Returns false (unknown cause, don't assume transient)
        assertThat(retryable).isFalse()
    }
    
    // ========================================
    // AC #5: Performance Tests
    // ========================================
    
    /**
     * AC #5: Error classification completes in < 10ms per exception
     * Test: Performance benchmark for classification
     */
    @Test
    fun `classify should complete in less than 10ms per exception`() {
        // Given: Various exception types
        val exceptions = listOf(
            IOException("Network error"),
            SocketTimeoutException("Timeout"),
            HttpException(Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))),
            HttpException(Response.error<Any>(401, "".toResponseBody("text/plain".toMediaType()))),
            JsonSyntaxException("Parse error"),
            SecurityException("Permission denied"),
            IllegalArgumentException("Invalid calories: out of range")
        )
        
        // When: Performance test over 100 iterations per exception type
        val iterations = 100
        val measurements = mutableListOf<Long>()
        
        exceptions.forEach { exception ->
            repeat(iterations) {
                val startTime = System.nanoTime()
                errorHandler.classify(exception)
                val endTime = System.nanoTime()
                measurements.add(endTime - startTime)
            }
        }
        
        val averageNanos = measurements.average()
        val averageMillis = averageNanos / 1_000_000.0
        
        // Then: Average execution time < 10ms
        assertThat(averageMillis).isLessThan(10.0)
    }
    
    // ========================================
    // AC #4, #6: NotificationContent Tests
    // ========================================
    
    /**
     * AC #4, #6: ErrorHandler generates NotificationContent for each error type
     * Test: NotificationContent creation for all error types
     */
    @Test
    fun `getNotificationContent should generate content for all error types`() {
        // Given: All error types
        val errorTypes = listOf(
            ErrorType.NetworkError(IOException()),
            ErrorType.ServerError(503, "Unavailable"),
            ErrorType.HealthConnectUnavailable,
            ErrorType.AuthError("Unauthorized"),
            ErrorType.RateLimitError(null),
            ErrorType.ParseError(JsonSyntaxException("")),
            ErrorType.ValidationError("calories", "invalid"),
            ErrorType.PermissionDenied(listOf("Health Connect")),
            ErrorType.UnknownError(RuntimeException())
        )
        
        // When: NotificationContent generated for each
        val notifications = errorTypes.map { errorHandler.getNotificationContent(it) }
        
        // Then: All notifications have title and message
        notifications.forEach { notification ->
            assertThat(notification.title).isNotEmpty()
            assertThat(notification.message).isNotEmpty()
        }
    }
    
    /**
     * AC #6: All error types map to actionable user guidance
     * Test: AuthError notification is ongoing and has action button
     */
    @Test
    fun `getNotificationContent for AuthError should be ongoing with action`() {
        // Given: AuthError
        val error = ErrorType.AuthError("Unauthorized")
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification is ongoing (requires user action)
        assertThat(notification.isOngoing).isTrue()
        assertThat(notification.actionText).isEqualTo("Open Settings")
    }
    
    /**
     * AC #6: All error types map to actionable user guidance
     * Test: PermissionDenied notification is ongoing with action button
     */
    @Test
    fun `getNotificationContent for PermissionDenied should be ongoing with action`() {
        // Given: PermissionDenied
        val error = ErrorType.PermissionDenied(listOf("Health Connect"))
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification is ongoing (requires user action)
        assertThat(notification.isOngoing).isTrue()
        assertThat(notification.actionText).isEqualTo("Grant Access")
    }
    
    /**
     * AC #4: ErrorHandler provides user-friendly error messages
     * Test: NetworkError notification is not ongoing (transient)
     */
    @Test
    fun `getNotificationContent for NetworkError should not be ongoing`() {
        // Given: NetworkError
        val error = ErrorType.NetworkError(IOException())
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification is not ongoing (transient error)
        assertThat(notification.isOngoing).isFalse()
    }
    
    // ========================================
    // Story 4.6: New Error Types Tests
    // ========================================
    
    /**
     * Story 4.6, AC #1: Camera permission denial handling
     * Test: CameraPermissionDenied returns correct user message
     */
    @Test
    fun `getUserMessage for CameraPermissionDenied should return camera access message`() {
        // Given: CameraPermissionDenied error
        val error = ErrorType.CameraPermissionDenied
        
        // When: User message generated
        val message = errorHandler.getUserMessage(error)
        
        // Then: Message is user-friendly and actionable
        assertThat(message).isEqualTo("Camera access required for meal tracking")
    }
    
    /**
     * Story 4.6, AC #1: Camera permission denial is non-retryable
     */
    @Test
    fun `isRetryable for CameraPermissionDenied should return false`() {
        // Given: CameraPermissionDenied error
        val error = ErrorType.CameraPermissionDenied
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Not retryable (requires user action)
        assertThat(retryable).isFalse()
    }
    
    /**
     * Story 4.6, AC #1: Camera permission notification includes settings action
     */
    @Test
    fun `getNotificationContent for CameraPermissionDenied should include settings action`() {
        // Given: CameraPermissionDenied error
        val error = ErrorType.CameraPermissionDenied
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification includes "Open Settings" action
        assertThat(notification.title).isEqualTo("Camera Permission Required")
        assertThat(notification.actionText).isEqualTo("Open Settings")
        assertThat(notification.isOngoing).isFalse()
    }
    
    /**
     * Story 4.6, AC #2: API key missing handling
     * Test: ApiKeyMissing returns correct user message
     */
    @Test
    fun `getUserMessage for ApiKeyMissing should return configuration message`() {
        // Given: ApiKeyMissing error
        val error = ErrorType.ApiKeyMissing
        
        // When: User message generated
        val message = errorHandler.getUserMessage(error)
        
        // Then: Message directs user to settings
        assertThat(message).isEqualTo("Configure Azure OpenAI key in Settings")
    }
    
    /**
     * Story 4.6, AC #2: API key missing is non-retryable
     */
    @Test
    fun `isRetryable for ApiKeyMissing should return false`() {
        // Given: ApiKeyMissing error
        val error = ErrorType.ApiKeyMissing
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Not retryable (requires user configuration)
        assertThat(retryable).isFalse()
    }
    
    /**
     * Story 4.6, AC #2: API key missing notification includes settings action
     */
    @Test
    fun `getNotificationContent for ApiKeyMissing should include settings action`() {
        // Given: ApiKeyMissing error
        val error = ErrorType.ApiKeyMissing
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification includes "Open Settings" action
        assertThat(notification.title).isEqualTo("Configuration Required")
        assertThat(notification.message).contains("Configure Azure OpenAI key in Settings")
        assertThat(notification.actionText).isEqualTo("Open Settings")
        assertThat(notification.isOngoing).isFalse()
    }
    
    /**
     * Story 4.6, AC #4: Storage full handling
     * Test: StorageFull returns correct user message
     */
    @Test
    fun `getUserMessage for StorageFull should return storage full message`() {
        // Given: StorageFull error
        val error = ErrorType.StorageFull
        
        // When: User message generated
        val message = errorHandler.getUserMessage(error)
        
        // Then: Message instructs user to free space
        assertThat(message).isEqualTo("Storage full. Free up space to continue.")
    }
    
    /**
     * Story 4.6, AC #4: Storage full is non-retryable
     */
    @Test
    fun `isRetryable for StorageFull should return false`() {
        // Given: StorageFull error
        val error = ErrorType.StorageFull
        
        // When: Retry check performed
        val retryable = errorHandler.isRetryable(error)
        
        // Then: Not retryable (requires user action to free space)
        assertThat(retryable).isFalse()
    }
    
    /**
     * Story 4.6, AC #4: Storage full notification is dismissible
     */
    @Test
    fun `getNotificationContent for StorageFull should be dismissible`() {
        // Given: StorageFull error
        val error = ErrorType.StorageFull
        
        // When: NotificationContent generated
        val notification = errorHandler.getNotificationContent(error)
        
        // Then: Notification is dismissible (not ongoing)
        assertThat(notification.title).isEqualTo("Storage Full")
        assertThat(notification.isOngoing).isFalse()
        assertThat(notification.actionText).isNull() // No action needed - user must free space manually
    }
}
