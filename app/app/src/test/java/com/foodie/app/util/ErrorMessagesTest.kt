package com.foodie.app.util

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Unit tests for ErrorMessages utility.
 *
 * Tests verify that each exception type maps to the correct user-friendly message
 * and that messages contain no technical jargon.
 */
class ErrorMessagesTest {

    @Test
    fun `toUserMessage maps UnknownHostException to network message`() {
        val exception = UnknownHostException("api.example.com")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("No internet connection. Please check your network.")
        assertThat(message).doesNotContain("UnknownHostException")
    }

    @Test
    fun `toUserMessage maps SocketTimeoutException to network message`() {
        val exception = SocketTimeoutException("timeout")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("No internet connection. Please check your network.")
    }

    @Test
    fun `toUserMessage maps IOException to network error message`() {
        val exception = IOException("Connection reset")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Network error. Please check your connection and try again.")
        assertThat(message).doesNotContain("IOException")
    }

    @Test
    fun `toUserMessage maps HttpException 401 to authentication message`() {
        val exception = HttpException(Response.error<Any>(401, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Authentication failed. Please check your API key in settings.")
        assertThat(message).doesNotContain("401")
        assertThat(message).doesNotContain("HttpException")
    }

    @Test
    fun `toUserMessage maps HttpException 403 to authentication message`() {
        val exception = HttpException(Response.error<Any>(403, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Authentication failed. Please check your API key in settings.")
    }

    @Test
    fun `toUserMessage maps HttpException 429 to rate limit message`() {
        val exception = HttpException(Response.error<Any>(429, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Too many requests. Please try again in a moment.")
        assertThat(message).doesNotContain("429")
    }

    @Test
    fun `toUserMessage maps HttpException 500 to server error message`() {
        val exception = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Server error. Please try again later.")
        assertThat(message).doesNotContain("500")
    }

    @Test
    fun `toUserMessage maps HttpException 503 to server error message`() {
        val exception = HttpException(Response.error<Any>(503, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Server error. Please try again later.")
    }

    @Test
    fun `toUserMessage maps HttpException 404 to generic request failed message`() {
        val exception = HttpException(Response.error<Any>(404, "".toResponseBody(null)))

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Request failed. Please try again.")
        assertThat(message).doesNotContain("404")
    }

    @Test
    fun `toUserMessage maps SecurityException to permission denied message`() {
        val exception = SecurityException("Permission denied")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Permission denied. Please grant Health Connect access in settings.")
        assertThat(message).doesNotContain("SecurityException")
    }

    @Test
    fun `toUserMessage maps IllegalStateException with Health Connect to unavailable message`() {
        val exception = IllegalStateException("Health Connect is not available")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Health Connect is not available. Please install it from the Play Store.")
    }

    @Test
    fun `toUserMessage maps generic IllegalStateException to restart app message`() {
        val exception = IllegalStateException("Invalid state")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Something went wrong. Please restart the app.")
        assertThat(message).doesNotContain("IllegalStateException")
    }

    @Test
    fun `toUserMessage maps IllegalArgumentException to invalid input message`() {
        val exception = IllegalArgumentException("Invalid parameter")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("Invalid input. Please check your data and try again.")
        assertThat(message).doesNotContain("IllegalArgumentException")
    }

    @Test
    fun `toUserMessage maps unknown exception to generic message`() {
        val exception = RuntimeException("Unexpected error")

        val message = ErrorMessages.toUserMessage(exception)

        assertThat(message).isEqualTo("An unexpected error occurred. Please try again.")
        assertThat(message).doesNotContain("RuntimeException")
        assertThat(message).doesNotContain("Unexpected error")
    }

    @Test
    fun `all error messages are under 50 words`() {
        val exceptions = listOf(
            UnknownHostException(),
            SocketTimeoutException(),
            IOException(),
            HttpException(Response.error<Any>(401, "".toResponseBody(null))),
            HttpException(Response.error<Any>(429, "".toResponseBody(null))),
            HttpException(Response.error<Any>(500, "".toResponseBody(null))),
            SecurityException(),
            IllegalStateException("Health Connect unavailable"),
            IllegalStateException("Generic error"),
            IllegalArgumentException(),
            RuntimeException(),
        )

        exceptions.forEach { exception ->
            val message = ErrorMessages.toUserMessage(exception)
            val wordCount = message.split("\\s+".toRegex()).size
            assertThat(wordCount).isAtMost(50)
        }
    }

    @Test
    fun `all error messages are actionable`() {
        val exceptions = listOf(
            UnknownHostException(),
            SecurityException(),
            IllegalStateException("Health Connect unavailable"),
            HttpException(Response.error<Any>(401, "".toResponseBody(null))),
        )

        exceptions.forEach { exception ->
            val message = ErrorMessages.toUserMessage(exception)
            // Actionable messages should contain guidance (check, grant, install, restart, etc.)
            assertThat(message.lowercase()).containsMatch("(check|grant|install|restart|try again)")
        }
    }
}
