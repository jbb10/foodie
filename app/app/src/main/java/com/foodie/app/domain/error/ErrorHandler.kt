package com.foodie.app.domain.error

import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import timber.log.Timber

/**
 * Error classification and user messaging utility.
 *
 * Classifies exceptions into structured ErrorType categories and generates
 * user-friendly error messages. Central error handling logic for all app components.
 *
 * Architecture Pattern:
 * - Repository catches exception → ErrorHandler.classify() → ErrorType
 * - ErrorHandler.getUserMessage() → user-facing error text
 * - ErrorHandler.isRetryable() → determines retry strategy
 * - ErrorHandler.getNotificationContent() → notification display data
 *
 * Performance Requirements:
 * - classify() must complete in < 10ms (verified in unit tests)
 * - Uses efficient sealed class pattern matching (no reflection)
 *
 * Security:
 * - User messages NEVER expose API keys, endpoints, or internal class names
 * - Technical details logged via Timber (DEBUG builds only)
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
@Singleton
class ErrorHandler @Inject constructor() {

    companion object {
        private const val TAG = "ErrorHandler"
    }

    /**
     * Classifies an exception into a structured ErrorType.
     *
     * Maps common exception types to domain-specific error categories:
     * - Network errors: IOException, SocketTimeoutException, UnknownHostException
     * - Server errors: HttpException 500-599
     * - Auth errors: HttpException 401/403
     * - Rate limit: HttpException 429
     * - Parse errors: JsonSyntaxException, JsonParseException
     * - Permission errors: SecurityException
     * - Validation errors: IllegalArgumentException (with context)
     * - Unknown: All other exceptions
     *
     * Performance: < 10ms execution time (pattern matching, no reflection)
     *
     * @param exception The caught exception to classify
     * @return ErrorType representing the error category
     */
    fun classify(exception: Throwable): ErrorType {
        // Log exception for debugging (DEBUG builds only)
        Timber.tag(TAG).d(exception, "Classifying exception: ${exception.javaClass.simpleName}")

        return when (exception) {
            // Network-related errors (retryable)
            is SocketTimeoutException -> {
                Timber.tag(TAG).d("Classified as NetworkError: Socket timeout")
                ErrorType.NetworkError(exception)
            }
            is UnknownHostException -> {
                Timber.tag(TAG).d("Classified as NetworkError: Unknown host")
                ErrorType.NetworkError(exception)
            }
            is IOException -> {
                Timber.tag(TAG).d("Classified as NetworkError: IO exception")
                ErrorType.NetworkError(exception)
            }

            // HTTP errors
            is HttpException -> {
                when (exception.code()) {
                    in 500..599 -> {
                        Timber.tag(TAG).d("Classified as ServerError: HTTP ${exception.code()}")
                        ErrorType.ServerError(
                            statusCode = exception.code(),
                            message = exception.message() ?: "Server error",
                        )
                    }
                    401, 403 -> {
                        Timber.tag(TAG).d("Classified as AuthError: HTTP ${exception.code()}")
                        ErrorType.AuthError(
                            message = exception.message() ?: "Authentication failed",
                        )
                    }
                    429 -> {
                        Timber.tag(TAG).d("Classified as RateLimitError: HTTP 429")
                        val retryAfter = extractRetryAfter(exception)
                        ErrorType.RateLimitError(retryAfter)
                    }
                    else -> {
                        Timber.tag(TAG).d("Classified as UnknownError: HTTP ${exception.code()}")
                        ErrorType.UnknownError(exception)
                    }
                }
            }

            // JSON parsing errors (non-retryable)
            is JsonSyntaxException, is JsonParseException -> {
                Timber.tag(TAG).d("Classified as ParseError: JSON parsing failed")
                ErrorType.ParseError(exception)
            }

            // Permission errors (non-retryable)
            is SecurityException -> {
                Timber.tag(TAG).d("Classified as PermissionDenied: Security exception")
                ErrorType.PermissionDenied(
                    permissions = extractPermissionsFromException(),
                )
            }

            // Validation errors (non-retryable)
            is IllegalArgumentException -> {
                // Check if this is a validation error with field context
                val (field, reason) = parseValidationError(exception)
                if (field != null && reason != null) {
                    Timber.tag(TAG).d("Classified as ValidationError: $field - $reason")
                    ErrorType.ValidationError(field, reason)
                } else {
                    Timber.tag(TAG).d("Classified as UnknownError: IllegalArgumentException without validation context")
                    ErrorType.UnknownError(exception)
                }
            }

            // Fallback for unknown exceptions
            else -> {
                Timber.tag(TAG).w("Classified as UnknownError: ${exception.javaClass.simpleName}")
                ErrorType.UnknownError(exception)
            }
        }
    }

    /**
     * Generates user-friendly error message for an ErrorType.
     *
     * Messages are:
     * - Non-technical (no jargon, class names, stack traces)
     * - Actionable (tell user what to do)
     * - Secure (no API keys, endpoints, sensitive data)
     *
     * @param error The classified error type
     * @return User-friendly error message
     */
    fun getUserMessage(error: ErrorType): String {
        return when (error) {
            is ErrorType.NetworkError ->
                "Request timed out. Check your internet connection."

            is ErrorType.ServerError ->
                "Service temporarily unavailable. Will retry automatically."

            is ErrorType.HealthConnectUnavailable ->
                "Health Connect required. Install from Play Store?"

            is ErrorType.AuthError ->
                "API key invalid. Check settings."

            is ErrorType.RateLimitError -> {
                if (error.retryAfter != null) {
                    "Too many requests. Please wait ${error.retryAfter} seconds."
                } else {
                    "Too many requests. Please wait a moment."
                }
            }

            is ErrorType.ParseError ->
                "Unexpected response from AI service."

            is ErrorType.ValidationError ->
                "Invalid ${error.field}: ${error.reason}"

            is ErrorType.PermissionDenied ->
                "Health Connect permissions required. Tap to grant access."

            is ErrorType.CameraPermissionDenied ->
                "Camera access required for meal tracking"

            is ErrorType.ApiKeyMissing ->
                "Configure Azure OpenAI key in Settings"

            is ErrorType.StorageFull ->
                "Storage full. Free up space to continue."

            is ErrorType.UnknownError ->
                "An unexpected error occurred. Please try again."
        }
    }

    /**
     * Determines if an error type is retryable.
     *
     * Retryable errors are transient failures that may succeed on retry:
     * - NetworkError (connectivity restored)
     * - ServerError (server recovers)
     * - HealthConnectUnavailable (user installs/enables)
     *
     * Non-retryable errors require user intervention or indicate permanent failure:
     * - AuthError (invalid API key)
     * - RateLimitError (quota exhausted, retry would worsen)
     * - ParseError (server bug or client incompatibility)
     * - ValidationError (invalid input)
     * - PermissionDenied (user must grant permissions)
     * - CameraPermissionDenied (user must grant camera access)
     * - ApiKeyMissing (user must configure API key)
     * - StorageFull (user must free up storage space)
     * - UnknownError (unknown cause, assume non-transient)
     *
     * @param error The classified error type
     * @return true if error should be retried, false otherwise
     */
    fun isRetryable(error: ErrorType): Boolean {
        return when (error) {
            // Retryable (transient failures)
            is ErrorType.NetworkError -> true
            is ErrorType.ServerError -> true
            is ErrorType.HealthConnectUnavailable -> true

            // Non-retryable (permanent failures or require user action)
            is ErrorType.AuthError -> false
            is ErrorType.RateLimitError -> false
            is ErrorType.ParseError -> false
            is ErrorType.ValidationError -> false
            is ErrorType.PermissionDenied -> false
            is ErrorType.CameraPermissionDenied -> false
            is ErrorType.ApiKeyMissing -> false
            is ErrorType.StorageFull -> false
            is ErrorType.UnknownError -> false
        }
    }

    /**
     * Generates notification content for an error type.
     *
     * Creates NotificationContent with appropriate title, message, and action
     * button for displaying error notifications to users.
     *
     * @param error The classified error type
     * @return NotificationContent for displaying the error
     */
    fun getNotificationContent(error: ErrorType): NotificationContent {
        return when (error) {
            is ErrorType.NetworkError ->
                NotificationContent.networkError()

            is ErrorType.ServerError ->
                NotificationContent.serverError()

            is ErrorType.HealthConnectUnavailable ->
                NotificationContent(
                    title = "Health Connect Required",
                    message = getUserMessage(error),
                    actionText = null,
                    actionIntent = null,
                    isOngoing = false,
                )

            is ErrorType.AuthError ->
                NotificationContent.authError(settingsIntent = null)

            is ErrorType.RateLimitError ->
                NotificationContent.rateLimit()

            is ErrorType.ParseError ->
                NotificationContent(
                    title = "Service Error",
                    message = getUserMessage(error),
                    actionText = null,
                    actionIntent = null,
                    isOngoing = false,
                )

            is ErrorType.ValidationError ->
                NotificationContent(
                    title = "Invalid Input",
                    message = getUserMessage(error),
                    actionText = null,
                    actionIntent = null,
                    isOngoing = false,
                )

            is ErrorType.PermissionDenied ->
                NotificationContent.permissionDenied(permissionsIntent = null)

            is ErrorType.CameraPermissionDenied ->
                NotificationContent(
                    title = "Camera Permission Required",
                    message = getUserMessage(error),
                    actionText = "Open Settings",
                    actionIntent = null, // Intent created by caller with context
                    isOngoing = false,
                )

            is ErrorType.ApiKeyMissing ->
                NotificationContent(
                    title = "Configuration Required",
                    message = getUserMessage(error),
                    actionText = "Open Settings",
                    actionIntent = null, // Intent created by caller with context
                    isOngoing = false,
                )

            is ErrorType.StorageFull ->
                NotificationContent(
                    title = "Storage Full",
                    message = getUserMessage(error),
                    actionText = null,
                    actionIntent = null,
                    isOngoing = false,
                )

            is ErrorType.UnknownError ->
                NotificationContent(
                    title = "Error",
                    message = getUserMessage(error),
                    actionText = null,
                    actionIntent = null,
                    isOngoing = false,
                )
        }
    }

    /**
     * Extracts retry-after header from HTTP 429 response.
     *
     * @param exception HttpException with 429 status code
     * @return Retry-after seconds, or null if header not present
     */
    private fun extractRetryAfter(exception: HttpException): Int? {
        return try {
            exception.response()?.headers()?.get("Retry-After")?.toIntOrNull()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to extract Retry-After header")
            null
        }
    }

    /**
     * Extracts permission names from SecurityException message.
     *
     * @return List of permission names, or generic list if extraction fails
     */
    private fun extractPermissionsFromException(): List<String> {
        // For now, return generic permission list
        // Future enhancement: Parse exception message for specific permissions
        return listOf("Health Connect")
    }

    /**
     * Parses validation error from IllegalArgumentException message.
     *
     * Expected message format: "Invalid {field}: {reason}"
     * Example: "Invalid calories: must be between 1 and 5000"
     *
     * @param exception IllegalArgumentException
     * @return Pair of (field, reason) or (null, null) if parsing fails
     */
    private fun parseValidationError(exception: IllegalArgumentException): Pair<String?, String?> {
        val message = exception.message ?: return Pair(null, null)

        // Try to parse "Invalid {field}: {reason}" pattern
        val invalidPattern = """Invalid (\w+): (.+)""".toRegex()
        val match = invalidPattern.find(message)

        return if (match != null && match.groupValues.size >= 3) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            Pair(null, null)
        }
    }
}
