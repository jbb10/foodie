package com.foodie.app.util

import timber.log.Timber

/**
 * Custom Timber tree for release builds.
 *
 * Only logs ERROR and WARN levels to prevent sensitive information leaks in production.
 * Debug and Info logs are suppressed in release builds.
 */
class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log WARNING and ERROR in release builds
        if (priority == android.util.Log.WARN || priority == android.util.Log.ERROR) {
            // In production, you might want to send to crashlytics or other logging service
            // For now, we just use standard logging
            if (priority == android.util.Log.ERROR && t != null) {
                android.util.Log.e(tag, message, t)
            } else if (priority == android.util.Log.ERROR) {
                android.util.Log.e(tag, message)
            } else {
                android.util.Log.w(tag, message)
            }
        }
    }
}

/**
 * Extension function for debug logging with automatic class tag.
 *
 * Usage: logDebug("User logged in")
 */
fun Any.logDebug(message: String) {
    Timber.tag(this::class.java.simpleName).d(message)
}

/**
 * Extension function for info logging with automatic class tag.
 *
 * Usage: logInfo("Data sync completed")
 */
fun Any.logInfo(message: String) {
    Timber.tag(this::class.java.simpleName).i(message)
}

/**
 * Extension function for warning logging with automatic class tag.
 *
 * Usage: logWarn("Deprecated API called")
 */
fun Any.logWarn(message: String) {
    Timber.tag(this::class.java.simpleName).w(message)
}

/**
 * Extension function for error logging with automatic class tag.
 *
 * Usage: logError(exception, "Failed to fetch data")
 *
 * @param throwable The exception that occurred (optional)
 * @param message Descriptive error message
 */
fun Any.logError(throwable: Throwable? = null, message: String) {
    if (throwable != null) {
        Timber.tag(this::class.java.simpleName).e(throwable, message)
    } else {
        Timber.tag(this::class.java.simpleName).e(message)
    }
}
