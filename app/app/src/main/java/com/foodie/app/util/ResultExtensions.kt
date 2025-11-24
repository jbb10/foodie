package com.foodie.app.util

import timber.log.Timber

/**
 * Wraps a block of code in try-catch and returns Result<T>.
 *
 * Automatically logs errors with Timber and converts exceptions to user-friendly messages.
 *
 * Usage:
 * ```
 * suspend fun fetchData(): Result<Data> = runCatchingResult {
 *     apiService.getData()
 * }
 * ```
 *
 * @param block The code block to execute
 * @return Result.Success with data on success, or Result.Error with user-friendly message on failure
 */
inline fun <T> runCatchingResult(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        val userMessage = ErrorMessages.toUserMessage(e)
        Timber.e(e, "Operation failed: $userMessage")
        Result.Error(e, userMessage)
    }
}

/**
 * Transforms the error message in a Result.Error.
 *
 * Useful for adding context or customizing error messages for specific use cases.
 *
 * Usage:
 * ```
 * repository.fetchData()
 *     .mapError { "Failed to load meal data: $it" }
 * ```
 *
 * @param transform Function to transform the error message
 * @return Original Result if Success, or Result.Error with transformed message if Error
 */
fun <T> Result<T>.mapError(transform: (String) -> String): Result<T> {
    return when (this) {
        is Result.Success -> this
        is Result.Error -> Result.Error(this.exception, transform(this.message))
        is Result.Loading -> this
    }
}

/**
 * Executes a side effect when the Result is Success.
 *
 * Returns the original Result to allow chaining.
 *
 * Usage:
 * ```
 * repository.saveData()
 *     .onSuccess { data -> 
 *         Timber.i("Data saved: $data")
 *     }
 *     .onError { throwable, message ->
 *         Timber.e(throwable, message)
 *     }
 * ```
 *
 * @param action The action to perform with the success data
 * @return The original Result
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(this.data)
    }
    return this
}

/**
 * Executes a side effect when the Result is Error.
 *
 * Returns the original Result to allow chaining.
 *
 * Usage:
 * ```
 * repository.fetchData()
 *     .onError { throwable, message ->
 *         Timber.e(throwable, "Failed: $message")
 *     }
 * ```
 *
 * @param action The action to perform with the exception and error message
 * @return The original Result
 */
inline fun <T> Result<T>.onError(action: (Throwable, String) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(this.exception, this.message)
    }
    return this
}
