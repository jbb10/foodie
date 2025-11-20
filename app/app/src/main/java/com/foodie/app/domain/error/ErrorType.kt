package com.foodie.app.domain.error

/**
 * Sealed class hierarchy representing all error types in the application.
 *
 * Categorizes errors into retryable (transient network/server issues) and
 * non-retryable (auth, validation, permissions). Used by ErrorHandler for
 * classification and user message generation.
 *
 * Architecture Pattern:
 * - Repository catches exceptions → ErrorHandler.classify() → ErrorType
 * - ErrorType determines retry policy and user messaging strategy
 * - Result.Error carries ErrorType for consistent error handling across app
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
sealed class ErrorType {
    
    // ========================================
    // RETRYABLE ERRORS (Transient Failures)
    // ========================================
    
    /**
     * Network connectivity error.
     *
     * Occurs when:
     * - No internet connection (WiFi/cellular unavailable)
     * - Request timeout (socket timeout, connection timeout)
     * - DNS resolution failure
     *
     * Mapped from: IOException, SocketTimeoutException, UnknownHostException
     *
     * Retry Strategy: Yes (with exponential backoff)
     * User Action: Check internet connection, wait for connectivity restoration
     */
    data class NetworkError(val cause: Throwable) : ErrorType()
    
    /**
     * Server-side error (HTTP 5xx).
     *
     * Occurs when:
     * - Server returns 500 Internal Server Error
     * - Server returns 502 Bad Gateway / 503 Service Unavailable
     * - Server returns 504 Gateway Timeout
     *
     * Mapped from: HttpException with status code 500-599
     *
     * Retry Strategy: Yes (transient server issues often resolve automatically)
     * User Action: Wait, app will retry automatically
     */
    data class ServerError(val statusCode: Int, val message: String) : ErrorType()
    
    /**
     * Health Connect temporarily unavailable.
     *
     * Occurs when:
     * - Health Connect SDK not installed
     * - Health Connect service temporarily unreachable
     * - Health Connect updating/syncing
     *
     * Mapped from: Health Connect SDK exceptions (RemoteException, etc.)
     *
     * Retry Strategy: Yes (may become available after install/restart)
     * User Action: Install Health Connect from Play Store, restart app
     */
    data object HealthConnectUnavailable : ErrorType()
    
    // ========================================
    // NON-RETRYABLE ERRORS (Permanent Failures)
    // ========================================
    
    /**
     * Authentication/Authorization error (HTTP 401/403).
     *
     * Occurs when:
     * - API key invalid or expired (401 Unauthorized)
     * - API key lacks required permissions (403 Forbidden)
     * - API key not configured
     *
     * Mapped from: HttpException with status code 401 or 403
     *
     * Retry Strategy: No (will fail again without user intervention)
     * User Action: Check API key in settings, verify configuration
     */
    data class AuthError(val message: String) : ErrorType()
    
    /**
     * Rate limit exceeded (HTTP 429).
     *
     * Occurs when:
     * - Too many requests to Azure OpenAI API in short period
     * - API quota exhausted
     *
     * Mapped from: HttpException with status code 429
     *
     * Retry Strategy: No (automatic retry would worsen rate limit)
     * User Action: Wait before retrying (respect retryAfter header if present)
     */
    data class RateLimitError(val retryAfter: Int?) : ErrorType()
    
    /**
     * JSON parsing error.
     *
     * Occurs when:
     * - API returns malformed JSON
     * - API contract changed (unexpected response structure)
     * - Response body empty when JSON expected
     *
     * Mapped from: JsonSyntaxException, JsonParseException, MalformedJsonException
     *
     * Retry Strategy: No (server issue or client bug, not transient)
     * User Action: Report issue, developer investigation needed
     */
    data class ParseError(val cause: Throwable) : ErrorType()
    
    /**
     * Data validation error.
     *
     * Occurs when:
     * - Calories outside valid range (1-5000 kcal)
     * - Invalid timestamp (future date, too far in past)
     * - Description blank or too long
     *
     * Mapped from: IllegalArgumentException with validation context
     *
     * Retry Strategy: No (user input needs correction)
     * User Action: Correct invalid field per validation message
     */
    data class ValidationError(val field: String, val reason: String) : ErrorType()
    
    /**
     * Permission denied error.
     *
     * Occurs when:
     * - Health Connect permissions not granted
     * - Health Connect permissions revoked by user
     * - Camera permission denied (photo capture)
     *
     * Mapped from: SecurityException, PermissionDeniedException
     *
     * Retry Strategy: No (requires user action)
     * User Action: Grant required permissions via system settings
     */
    data class PermissionDenied(val permissions: List<String>) : ErrorType()
    
    /**
     * Camera permission denied error.
     *
     * Occurs when:
     * - User denies camera permission
     * - Camera permission revoked in system settings
     *
     * Retry Strategy: No (requires user action)
     * User Action: Grant camera permission via app settings
     *
     * Story: 4.6 - Graceful Degradation (AC#1)
     */
    data object CameraPermissionDenied : ErrorType()
    
    /**
     * API key missing or not configured.
     *
     * Occurs when:
     * - Azure OpenAI API key is null or empty
     * - User hasn't configured API key in settings
     *
     * Retry Strategy: No (requires user configuration)
     * User Action: Configure Azure OpenAI key in Settings
     *
     * Story: 4.6 - Graceful Degradation (AC#2)
     */
    data object ApiKeyMissing : ErrorType()
    
    /**
     * Storage space full or insufficient.
     *
     * Occurs when:
     * - Available storage < 10MB threshold
     * - Device storage critically low
     *
     * Retry Strategy: No (requires user action to free space)
     * User Action: Free up device storage space
     *
     * Story: 4.6 - Graceful Degradation (AC#4)
     */
    data object StorageFull : ErrorType()
    
    /**
     * Unknown/unexpected error.
     *
     * Fallback for exceptions that don't match specific error types.
     * Should be rare - most exceptions should map to specific ErrorType.
     *
     * Retry Strategy: No (unknown cause, don't assume transient)
     * User Action: Report issue, restart app
     */
    data class UnknownError(val cause: Throwable) : ErrorType()
}
