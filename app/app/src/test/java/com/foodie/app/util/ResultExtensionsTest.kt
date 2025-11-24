package com.foodie.app.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for Result extension functions.
 *
 * Tests verify runCatchingResult, mapError, onSuccess, and onError helpers.
 */
class ResultExtensionsTest {

    @Test
    fun `runCatchingResult returns Success when block succeeds`() {
        val result = runCatchingResult {
            "Success data"
        }

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo("Success data")
    }

    @Test
    fun `runCatchingResult returns Error when block throws exception`() {
        val exception = IOException("Network error")

        val result = runCatchingResult {
            throw exception
        }

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(exception)
        assertThat(error.message).isEqualTo("Network error. Please check your connection and try again.")
    }

    @Test
    fun `runCatchingResult maps exception to user-friendly message`() {
        val exception = SecurityException("Permission denied")

        val result = runCatchingResult {
            throw exception
        }

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).isEqualTo("Permission denied. Please grant Health Connect access in settings.")
        assertThat(error.message).doesNotContain("SecurityException")
    }

    @Test
    fun `mapError transforms error message in Result Error`() {
        val originalError = Result.Error(IOException(), "Network error")

        val transformedResult = originalError.mapError { "Custom: $it" }

        assertThat(transformedResult).isInstanceOf(Result.Error::class.java)
        val error = transformedResult as Result.Error
        assertThat(error.message).isEqualTo("Custom: Network error")
    }

    @Test
    fun `mapError does not modify Success result`() {
        val successResult = Result.Success("data")

        val transformedResult = successResult.mapError { "Should not be called" }

        assertThat(transformedResult).isInstanceOf(Result.Success::class.java)
        assertThat((transformedResult as Result.Success).data).isEqualTo("data")
    }

    @Test
    fun `mapError does not modify Loading result`() {
        val loadingResult = Result.Loading

        val transformedResult = loadingResult.mapError { "Should not be called" }

        assertThat(transformedResult).isInstanceOf(Result.Loading::class.java)
    }

    @Test
    fun `onSuccess executes action when Result is Success`() {
        val successResult = Result.Success("test data")
        var capturedData: String? = null

        successResult.onSuccess { data ->
            capturedData = data
        }

        assertThat(capturedData).isEqualTo("test data")
    }

    @Test
    fun `onSuccess does not execute action when Result is Error`() {
        val errorResult = Result.Error(IOException(), "error")
        var actionExecuted = false

        errorResult.onSuccess {
            actionExecuted = true
        }

        assertThat(actionExecuted).isFalse()
    }

    @Test
    fun `onSuccess returns original Result for chaining`() {
        val successResult = Result.Success("data")

        val returnedResult = successResult.onSuccess { }

        assertThat(returnedResult).isSameInstanceAs(successResult)
    }

    @Test
    fun `onError executes action when Result is Error`() {
        val exception = IOException("test error")
        val errorResult = Result.Error(exception, "Network error")
        var capturedThrowable: Throwable? = null
        var capturedMessage: String? = null

        errorResult.onError { throwable, message ->
            capturedThrowable = throwable
            capturedMessage = message
        }

        assertThat(capturedThrowable).isEqualTo(exception)
        assertThat(capturedMessage).isEqualTo("Network error")
    }

    @Test
    fun `onError does not execute action when Result is Success`() {
        val successResult = Result.Success("data")
        var actionExecuted = false

        successResult.onError { _, _ ->
            actionExecuted = true
        }

        assertThat(actionExecuted).isFalse()
    }

    @Test
    fun `onError returns original Result for chaining`() {
        val errorResult = Result.Error(IOException(), "error")

        val returnedResult = errorResult.onError { _, _ -> }

        assertThat(returnedResult).isSameInstanceAs(errorResult)
    }

    @Test
    fun `onSuccess and onError can be chained`() {
        val successResult = Result.Success("data")
        var successCalled = false
        var errorCalled = false

        successResult
            .onSuccess { successCalled = true }
            .onError { _, _ -> errorCalled = true }

        assertThat(successCalled).isTrue()
        assertThat(errorCalled).isFalse()
    }

    @Test
    fun `runCatchingResult with suspend function`() = runTest {
        suspend fun fetchData(): String {
            return "async data"
        }

        val result = runCatchingResult {
            fetchData()
        }

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo("async data")
    }

    @Test
    fun `runCatchingResult catches exception from suspend function`() = runTest {
        suspend fun fetchData(): String {
            throw IOException("Network failed")
        }

        val result = runCatchingResult {
            fetchData()
        }

        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IOException::class.java)
    }
}
