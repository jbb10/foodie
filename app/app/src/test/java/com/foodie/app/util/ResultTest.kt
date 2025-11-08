package com.foodie.app.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for Result wrapper class and Flow.asResult() extension.
 */
class ResultTest {

    @Test
    fun `Success getOrNull should return data`() {
        // Given
        val data = "test data"
        val result = Result.Success(data)

        // When
        val value = result.getOrNull()

        // Then
        assertThat(value).isEqualTo(data)
    }

    @Test
    fun `Error getOrNull should return null`() {
        // Given
        val result: Result<String> = Result.Error(RuntimeException("error"))

        // When
        val value = result.getOrNull()

        // Then
        assertThat(value).isNull()
    }

    @Test
    fun `Loading getOrNull should return null`() {
        // Given
        val result: Result<String> = Result.Loading

        // When
        val value = result.getOrNull()

        // Then
        assertThat(value).isNull()
    }

    @Test
    fun `Error exceptionOrNull should return exception`() {
        // Given
        val exception = RuntimeException("error")
        val result = Result.Error(exception)

        // When
        val returnedException = result.exceptionOrNull()

        // Then
        assertThat(returnedException).isEqualTo(exception)
    }

    @Test
    fun `Success exceptionOrNull should return null`() {
        // Given
        val result = Result.Success("data")

        // When
        val exception: Throwable? = result.exceptionOrNull()

        // Then
        assertThat(exception).isNull()
    }

    @Test
    fun `Loading exceptionOrNull should return null`() {
        // Given
        val result = Result.Loading

        // When
        val exception: Throwable? = result.exceptionOrNull()

        // Then
        assertThat(exception).isNull()
    }

    @Test
    fun `Success isSuccess should return true`() {
        // Given
        val result = Result.Success("data")

        // When/Then
        assertThat(result.isSuccess()).isTrue()
    }

    @Test
    fun `Error isSuccess should return false`() {
        // Given
        val result = Result.Error(RuntimeException("error"))

        // When/Then
        assertThat(result.isSuccess()).isFalse()
    }

    @Test
    fun `Loading isSuccess should return false`() {
        // Given
        val result = Result.Loading

        // When/Then
        assertThat(result.isSuccess()).isFalse()
    }

    @Test
    fun `Error isError should return true`() {
        // Given
        val result = Result.Error(RuntimeException("error"))

        // When/Then
        assertThat(result.isError()).isTrue()
    }

    @Test
    fun `Success isError should return false`() {
        // Given
        val result = Result.Success("data")

        // When/Then
        assertThat(result.isError()).isFalse()
    }

    @Test
    fun `Loading isError should return false`() {
        // Given
        val result = Result.Loading

        // When/Then
        assertThat(result.isError()).isFalse()
    }

    @Test
    fun `Loading isLoading should return true`() {
        // Given
        val result = Result.Loading

        // When/Then
        assertThat(result.isLoading()).isTrue()
    }

    @Test
    fun `Success isLoading should return false`() {
        // Given
        val result = Result.Success("data")

        // When/Then
        assertThat(result.isLoading()).isFalse()
    }

    @Test
    fun `Error isLoading should return false`() {
        // Given
        val result = Result.Error(RuntimeException("error"))

        // When/Then
        assertThat(result.isLoading()).isFalse()
    }

    @Test
    fun `Error should use exception message by default`() {
        // Given
        val exception = RuntimeException("test error message")

        // When
        val result = Result.Error(exception)

        // Then
        assertThat(result.message).isEqualTo("test error message")
    }

    @Test
    fun `Error should allow custom message`() {
        // Given
        val exception = RuntimeException("technical error")
        val customMessage = "User-friendly error message"

        // When
        val result = Result.Error(exception, customMessage)

        // Then
        assertThat(result.message).isEqualTo(customMessage)
        assertThat(result.exception.message).isEqualTo("technical error")
    }

    @Test
    fun `Error should use default message when exception has no message`() {
        // Given
        val exception = RuntimeException()

        // When
        val result = Result.Error(exception)

        // Then
        assertThat(result.message).isEqualTo("Unknown error")
    }

    @Test
    fun `asResult should emit Loading then Success for successful flow`() = runTest {
        // Given
        val testData = listOf("item1", "item2", "item3")
        val testFlow = flow {
            emit(testData)
        }

        // When
        val results = testFlow.asResult().toList()

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Result.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Result.Success::class.java)
        assertThat((results[1] as Result.Success).data).isEqualTo(testData)
    }

    @Test
    fun `asResult should emit Loading then Error for failing flow`() = runTest {
        // Given
        val exception = RuntimeException("test error")
        val testFlow = flow<String> {
            throw exception
        }

        // When
        val results = testFlow.asResult().toList()

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Result.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Result.Error::class.java)
        assertThat((results[1] as Result.Error).exception).isEqualTo(exception)
    }

    @Test
    fun `asResult should emit Loading then multiple Success for multi-emission flow`() = runTest {
        // Given
        val testFlow = flow {
            emit("first")
            emit("second")
            emit("third")
        }

        // When
        val results = testFlow.asResult().toList()

        // Then
        assertThat(results).hasSize(4) // Loading + 3 Success
        assertThat(results[0]).isInstanceOf(Result.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Result.Success::class.java)
        assertThat(results[2]).isInstanceOf(Result.Success::class.java)
        assertThat(results[3]).isInstanceOf(Result.Success::class.java)
        assertThat((results[1] as Result.Success).data).isEqualTo("first")
        assertThat((results[2] as Result.Success).data).isEqualTo("second")
        assertThat((results[3] as Result.Success).data).isEqualTo("third")
    }

    @Test
    fun `asResult should catch exception in the middle of emissions`() = runTest {
        // Given
        val exception = RuntimeException("error in middle")
        val testFlow = flow {
            emit("first")
            emit("second")
            throw exception
        }

        // When
        val results = testFlow.asResult().toList()

        // Then
        assertThat(results).hasSize(4) // Loading + 2 Success + 1 Error
        assertThat(results[0]).isInstanceOf(Result.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Result.Success::class.java)
        assertThat(results[2]).isInstanceOf(Result.Success::class.java)
        assertThat(results[3]).isInstanceOf(Result.Error::class.java)
        assertThat((results[3] as Result.Error).exception).isEqualTo(exception)
    }
}
