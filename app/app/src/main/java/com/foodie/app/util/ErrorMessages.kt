package com.foodie.app.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Maps technical exceptions to user-friendly error messages.
 *
 * This object centralizes all error message mapping to ensure consistent,
 * jargon-free messages are shown to users across the application.
 */
object ErrorMessages {

    /**
     * Converts a throwable exception to a user-friendly message.
     *
     * Maps common exceptions to specific guidance messages, with a generic
     * fallback for unknown errors.
     *
     * Usage:
     * ```
     * try {
     *     performOperation()
     * } catch (e: Exception) {
     *     val message = ErrorMessages.toUserMessage(e)
     *     showError(message)
     * }
     * ```
     *
     * @param exception The exception to convert
     * @return User-friendly error message without technical jargon
     */
    fun toUserMessage(exception: Throwable): String {
        return when (exception) {
            // Network errors
            is UnknownHostException, is SocketTimeoutException ->
                "No internet connection. Please check your network."

            is IOException ->
                "Network error. Please check your connection and try again."

            // HTTP errors (API calls)
            is HttpException -> {
                when (exception.code()) {
                    401, 403 -> "Authentication failed. Please check your API key in settings."
                    429 -> "Too many requests. Please try again in a moment."
                    in 500..599 -> "Server error. Please try again later."
                    else -> "Request failed. Please try again."
                }
            }

            // Health Connect specific errors
            is SecurityException ->
                "Permission denied. Please grant Health Connect access in settings."

            is IllegalStateException -> {
                // Check if it's Health Connect unavailable
                if (exception.message?.contains("Health Connect", ignoreCase = true) == true) {
                    "Health Connect is not available. Please install it from the Play Store."
                } else {
                    "Something went wrong. Please restart the app."
                }
            }

            // IllegalArgumentException (bad input)
            is IllegalArgumentException ->
                "Invalid input. Please check your data and try again."

            // Generic fallback
            else -> "An unexpected error occurred. Please try again."
        }
    }
}
