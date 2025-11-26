package com.foodie.app.data.local.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Utility class for managing photo files in the app cache directory.
 *
 * Responsibilities:
 * - Create temporary photo files for camera capture
 * - Resize and compress photos to meet API requirements (2MP max, 80% JPEG quality)
 * - Delete photos after successful processing
 * - Provide FileProvider URIs for camera intent
 *
 * Storage location: `context.cacheDir/photos/`
 * Filename pattern: `meal_{timestamp}.jpg`
 *
 * Architecture:
 * - Injected via Hilt as singleton
 * - All operations suspend functions for coroutine integration
 * - Uses Dispatchers.IO for file operations
 *
 * Usage:
 * ```
 * val photoUri = photoManager.createPhotoFile()
 * // ... capture photo to photoUri ...
 * val processedUri = photoManager.resizeAndCompress(photoUri)
 * // ... send to API ...
 * photoManager.deletePhoto(processedUri)
 * ```
 */
@Singleton
class PhotoManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PHOTOS_DIR = "photos"
        private const val FILE_PROVIDER_AUTHORITY = "com.foodie.app.fileprovider"
        private const val MAX_PIXELS = 2_000_000 // 2MP (e.g., 1920x1080 = 2,073,600)
        private const val JPEG_QUALITY = 80 // 80% compression
    }

    /**
     * Creates a temporary file for camera capture and returns its FileProvider URI.
     *
     * The file is created in the cache directory with a timestamp-based filename.
     * The returned URI is suitable for use with camera intents.
     *
     * @return FileProvider URI pointing to the created temporary file
     */
    suspend fun createPhotoFile(): Uri = withContext(Dispatchers.IO) {
        val photosDir = File(context.cacheDir, PHOTOS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }

        val filename = generateFilename()
        val photoFile = File(photosDir, filename)

        Timber.d("Created photo file: ${photoFile.absolutePath}")

        FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, photoFile)
    }

    /**
     * Resizes and compresses a photo to meet API requirements.
     *
     * Processing steps:
     * 1. Read photo dimensions without full decode (memory-efficient)
     * 2. Calculate inSampleSize for initial downsampling if needed
     * 3. Decode photo from URI with inSampleSize optimization
     * 4. Apply EXIF orientation correction
     * 5. Resize if still exceeds 2MP limit (preserves aspect ratio)
     * 6. Compress to JPEG at 80% quality
     * 7. Save to new file in cache directory
     *
     * Memory optimization: Uses BitmapFactory.Options.inSampleSize to decode
     * large images at reduced resolution, preventing OOM on low-memory devices.
     *
     * @param sourceUri URI of the original photo (from camera capture)
     * @return URI of the processed photo, or null if processing fails
     */
    suspend fun resizeAndCompress(sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            // First pass: Read image dimensions without decoding full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            if (imageWidth <= 0 || imageHeight <= 0) {
                Timber.e("Failed to read image dimensions from URI: $sourceUri")
                return@withContext null
            }

            // Calculate inSampleSize for memory-efficient decode
            // If image is 12MP (4000x3000), inSampleSize=2 decodes at 2000x1500 (3MP)
            val inSampleSize = calculateInSampleSize(imageWidth, imageHeight, MAX_PIXELS)

            // Second pass: Decode bitmap with inSampleSize optimization
            options.apply {
                inJustDecodeBounds = false
                this.inSampleSize = inSampleSize
            }

            val originalBitmap = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (originalBitmap == null) {
                Timber.e("Failed to decode bitmap from URI: $sourceUri")
                return@withContext null
            }

            // Apply EXIF orientation correction
            val correctedBitmap = correctOrientation(sourceUri, originalBitmap)

            // Resize if exceeds 2MP
            val resizedBitmap = resizeTo2MP(correctedBitmap)

            // Save compressed photo
            val processedFile = File(context.cacheDir, "$PHOTOS_DIR/${generateFilename()}")
            FileOutputStream(processedFile).use { output ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }

            // Cleanup bitmaps
            if (correctedBitmap != originalBitmap) correctedBitmap.recycle()
            resizedBitmap.recycle()
            originalBitmap.recycle()

            val processedUri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                processedFile,
            )

            Timber.d(
                "Photo processed: ${processedFile.length() / 1024}KB " +
                    "(${resizedBitmap.width}x${resizedBitmap.height})",
            )

            processedUri
        } catch (e: IOException) {
            Timber.e(e, "Failed to resize and compress photo")
            null
        }
    }

    /**
     * Deletes a photo file from the cache directory.
     *
     * @param photoUri URI of the photo to delete
     * @return true if deletion succeeded, false otherwise
     */
    suspend fun deletePhoto(photoUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getFileFromUri(photoUri)
            val deleted = file?.delete() ?: false

            if (deleted) {
                Timber.d("Deleted photo: ${file?.absolutePath}")
            } else {
                Timber.w("Failed to delete photo or file not found: $photoUri")
            }

            deleted
        } catch (e: Exception) {
            Timber.e(e, "Error deleting photo: $photoUri")
            false
        }
    }

    /**
     * Gets the cache directory path for photo storage.
     *
     * @return File object representing the photos cache directory
     */
    fun getCacheDir(): File {
        return File(context.cacheDir, PHOTOS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Generates a unique filename using current timestamp.
     *
     * Format: `meal_{epochMillis}.jpg`
     * Example: `meal_1699564800000.jpg`
     *
     * @return Timestamped filename string
     */
    fun generateFilename(): String {
        return "meal_${System.currentTimeMillis()}.jpg"
    }

    /**
     * Corrects image orientation based on EXIF metadata.
     *
     * Many cameras save photos in landscape orientation but include EXIF rotation tags.
     * This function reads the EXIF orientation and applies the necessary rotation/flip.
     *
     * @param uri URI of the image file
     * @param bitmap Original bitmap to correct
     * @return Corrected bitmap (may be same instance if no correction needed)
     */
    private fun correctOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap // No correction needed
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            Timber.w(e, "Failed to read EXIF orientation, using original bitmap")
            return bitmap
        }
    }

    /**
     * Calculates optimal inSampleSize for memory-efficient bitmap decoding.
     *
     * inSampleSize is a power of 2 that tells BitmapFactory to decode every Nth pixel,
     * reducing memory usage significantly. For example:
     * - inSampleSize=1: Full resolution (default)
     * - inSampleSize=2: 1/4 memory (decode every 2nd pixel in each dimension)
     * - inSampleSize=4: 1/16 memory (decode every 4th pixel in each dimension)
     *
     * This function finds the largest inSampleSize value that is a power of 2 and keeps
     * the decoded image larger than or close to the target pixel count.
     *
     * Example: 12MP image (4000x3000) targeting 2MP:
     * - inSampleSize=2 → 2000x1500 = 3MP (acceptable, will resize to 2MP after)
     * - inSampleSize=4 → 1000x750 = 0.75MP (too small, already below target)
     *
     * @param width Original image width in pixels
     * @param height Original image height in pixels
     * @param maxPixels Target maximum pixel count (e.g., 2,000,000 for 2MP)
     * @return inSampleSize value (power of 2, minimum 1)
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxPixels: Int): Int {
        var inSampleSize = 1
        val currentPixels = width * height

        if (currentPixels > maxPixels) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            var halfWidth = width / 2
            var halfHeight = height / 2

            while ((halfWidth * halfHeight) >= maxPixels) {
                inSampleSize *= 2
                halfWidth /= 2
                halfHeight /= 2
            }
        }

        Timber.d(
            "Image ${width}x$height (${currentPixels / 1_000_000.0}MP) " +
                "→ inSampleSize=$inSampleSize " +
                "→ decoded as ${width / inSampleSize}x${height / inSampleSize} " +
                "(${(width / inSampleSize * height / inSampleSize) / 1_000_000.0}MP)",
        )

        return inSampleSize
    }

    /**
     * Resizes a bitmap to maximum 2MP resolution while preserving aspect ratio.
     *
     * If the bitmap is already under 2MP, returns the original.
     * Otherwise, calculates scale factor and creates downsampled bitmap.
     *
     * Algorithm:
     * - Calculate current total pixels (width * height)
     * - If <= 2MP: return original
     * - Calculate scale factor: sqrt(MAX_PIXELS / currentPixels)
     * - Apply scale to both dimensions
     *
     * @param bitmap Original bitmap to resize
     * @return Resized bitmap or original if already under 2MP
     */
    private fun resizeTo2MP(bitmap: Bitmap): Bitmap {
        val currentPixels = bitmap.width * bitmap.height

        if (currentPixels <= MAX_PIXELS) {
            Timber.d("Photo already <= 2MP (${bitmap.width}x${bitmap.height}), no resize needed")
            return bitmap
        }

        val scale = sqrt(MAX_PIXELS.toDouble() / currentPixels).toFloat()
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        Timber.d(
            "Resizing photo from ${bitmap.width}x${bitmap.height} " +
                "to ${newWidth}x$newHeight",
        )

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Converts a FileProvider URI back to a File object.
     *
     * @param uri FileProvider URI
     * @return File object or null if conversion fails
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            // FileProvider URIs have path like: content://authority/photos/meal_123.jpg
            // Extract the filename from the last path segment
            val filename = uri.lastPathSegment ?: return null
            File(getCacheDir(), filename)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get file from URI: $uri")
            null
        }
    }

    /**
     * Gets current cache statistics for monitoring storage usage.
     *
     * Scans the photos cache directory and calculates:
     * - Total size in bytes of all photos
     * - Number of photos in cache
     * - Age of oldest photo in hours
     *
     * Useful for:
     * - Debugging storage issues
     * - Monitoring cleanup effectiveness
     * - Settings screen display
     *
     * Story 4.4: Photo Retention and Cleanup
     *
     * @return CacheStats containing current cache metrics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir()

        if (!cacheDir.exists()) {
            return@withContext CacheStats(
                totalSizeBytes = 0L,
                photoCount = 0,
                oldestPhotoAgeHours = null,
            )
        }

        val files = cacheDir.listFiles() ?: emptyArray()

        if (files.isEmpty()) {
            return@withContext CacheStats(
                totalSizeBytes = 0L,
                photoCount = 0,
                oldestPhotoAgeHours = null,
            )
        }

        val totalSize = files.sumOf { it.length() }
        val currentTime = System.currentTimeMillis()

        val oldestFile = files.minByOrNull { it.lastModified() }
        val oldestAgeHours = oldestFile?.let {
            ((currentTime - it.lastModified()) / (1000 * 60 * 60)).toInt()
        }

        CacheStats(
            totalSizeBytes = totalSize,
            photoCount = files.size,
            oldestPhotoAgeHours = oldestAgeHours,
        )
    }
}

/**
 * Data class representing photo cache statistics.
 *
 * Story 4.4: Photo Retention and Cleanup
 *
 * @property totalSizeBytes Total size of all photos in cache directory (bytes)
 * @property photoCount Number of photos currently in cache
 * @property oldestPhotoAgeHours Age of oldest photo in hours, null if no photos
 */
data class CacheStats(
    val totalSizeBytes: Long,
    val photoCount: Int,
    val oldestPhotoAgeHours: Int?,
)
