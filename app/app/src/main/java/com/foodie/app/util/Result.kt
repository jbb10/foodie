package com.foodie.app.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * A generic wrapper for handling operation results with loading, success, and error states.
 *
 * Used across all repository operations for consistent error handling and state management.
 *
 * @param T The type of data wrapped in the result
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     *
     * @param data The successfully retrieved or processed data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with error information.
     *
     * @param exception The original exception that caused the failure
     * @param message User-friendly error message
     */
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : Result<Nothing>()

    /**
     * Represents an operation in progress.
     */
    data object Loading : Result<Nothing>()

    /**
     * Returns the data if this is a Success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the exception if this is an Error, null otherwise.
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }

    /**
     * Returns true if this is a Success result.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if this is an Error result.
     */
    fun isError(): Boolean = this is Error

    /**
     * Returns true if this is a Loading result.
     */
    fun isLoading(): Boolean = this is Loading
}

/**
 * Extension function to wrap a Flow<T> into a Flow<Result<T>>.
 *
 * Automatically emits Loading before the first value, wraps emissions in Success,
 * and catches exceptions as Error.
 *
 * Usage:
 * ```
 * flow {
 *     emit(fetchData())
 * }.asResult()
 * ```
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }
