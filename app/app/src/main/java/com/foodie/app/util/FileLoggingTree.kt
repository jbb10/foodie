package com.foodie.app.util

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber tree that logs to a persistent file on device storage.
 *
 * Logs are written to app's private files directory (accessible via adb pull).
 * Implements rolling buffer - when file exceeds maxFileSize, oldest entries are trimmed.
 *
 * This enables debugging production issues that occur when device is not connected
 * to development machine. Logs persist across app restarts and device reboots.
 *
 * Usage:
 * ```
 * // In FoodieApplication.onCreate()
 * if (BuildConfig.DEBUG) {
 *     Timber.plant(FileLoggingTree(this))
 * }
 *
 * // Pull logs from device
 * adb pull /data/data/com.foodie.app/files/foodie_logs.txt
 * // Or use: ./scripts/pull-logs.sh --app-file
 * ```
 *
 * File location: /data/data/com.foodie.app/files/foodie_logs.txt
 * Format: MM-dd HH:mm:ss.SSS LEVEL/Tag: message [exception stacktrace]
 *
 * Performance:
 * - File I/O is synchronous but fast (~1-2ms per log line)
 * - Rolling buffer prevents unbounded growth
 * - No impact on app responsiveness (logging is fire-and-forget)
 *
 * @param context Application context for accessing filesDir
 * @param maxFileSize Maximum file size in bytes before rotation (default: 20MB)
 * @param maxLines Maximum number of log lines to retain during rotation (default: 100k lines)
 */
class FileLoggingTree(
    context: Context,
    private val maxFileSize: Long = 20 * 1024 * 1024, // 20MB default
    private val maxLines: Int = 100_000, // ~100k lines default
) : Timber.Tree() {

    private val logFile = File(context.filesDir, "foodie_logs.txt")
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        // Create log file if doesn't exist
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
                logFile.writeText("=== Foodie Persistent Logs ===\n")
                logFile.appendText("Log file created: ${dateFormat.format(Date())}\n\n")
            } catch (e: Exception) {
                // Can't log this failure, just continue
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val timestamp = dateFormat.format(Date())
            val priorityChar = when (priority) {
                Log.VERBOSE -> 'V'
                Log.DEBUG -> 'D'
                Log.INFO -> 'I'
                Log.WARN -> 'W'
                Log.ERROR -> 'E'
                Log.ASSERT -> 'F'
                else -> '?'
            }

            // Format: MM-dd HH:mm:ss.SSS LEVEL/Tag: message
            val logLine = buildString {
                append(timestamp)
                append(" ")
                append(priorityChar)
                append("/")
                append(tag ?: "null")
                append(": ")
                append(message)

                // Append exception stack trace if present
                if (t != null) {
                    append("\n")
                    append(t.stackTraceToString())
                }

                append("\n")
            }

            // Append to file (synchronized to prevent concurrent write corruption)
            synchronized(this) {
                logFile.appendText(logLine)

                // Check if rotation needed
                if (logFile.length() > maxFileSize) {
                    rotateLog()
                }
            }
        } catch (e: Exception) {
            // Can't log the logging failure, just silently fail
            // This prevents infinite loop if file I/O fails
        }
    }

    /**
     * Rotates log file by keeping only the last N lines.
     *
     * When file exceeds maxFileSize, reads all lines, keeps last maxLines entries,
     * and overwrites file with trimmed content.
     *
     * This prevents unbounded log file growth while preserving recent history.
     */
    private fun rotateLog() {
        try {
            val lines = logFile.readLines()
            if (lines.size > maxLines) {
                // Keep last maxLines + header
                val trimmedLines = listOf(
                    "=== Foodie Persistent Logs ===",
                    "Log rotated: ${dateFormat.format(Date())} (kept last $maxLines lines)",
                    "",
                ) + lines.takeLast(maxLines)

                logFile.writeText(trimmedLines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            // If rotation fails, just clear the file to prevent unbounded growth
            try {
                logFile.writeText("=== Foodie Persistent Logs ===\n")
                logFile.appendText("Log rotation failed, file cleared: ${dateFormat.format(Date())}\n\n")
            } catch (clearException: Exception) {
                // Nothing we can do, file system may be full or permissions issue
            }
        }
    }

    /**
     * Returns the log file path for external access.
     *
     * Use this to expose log file location to Settings screen or debugging tools.
     *
     * @return File handle to persistent log file
     */
    fun getLogFile(): File = logFile
}
