package com.foodie.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import timber.log.Timber

/**
 * Utility class for encoding images to base64 data URLs for Azure OpenAI Responses API.
 *
 * Handles reading images from URIs (content:// or file:// schemes), encoding to base64,
 * and formatting as data URLs for multimodal API requests.
 *
 * **Memory Management:**
 * - Recycles bitmaps after encoding to prevent memory leaks
 * - Expects input images already compressed (2MP max resolution, 80% JPEG quality)
 * - Uses ByteArrayOutputStream for efficient byte handling
 *
 * **Data URL Format:**
 * Output format: `data:image/jpeg;base64,{base64_encoded_bytes}`
 * Example: `data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAA...`
 *
 * **Integration with PhotoManager:**
 * - PhotoManager (Story 2-3) provides photos already compressed to 2MP + 80% JPEG
 * - Typical file size: 200-500KB (compressed JPEG)
 * - Base64 encoding increases size ~33% → ~265-665KB encoded
 * - Azure OpenAI accepts multimodal requests up to several MB
 *
 * **Error Handling:**
 * - IOException if URI inaccessible or file read fails
 * - Returns null on encoding errors (caller should handle gracefully)
 *
 * Reference: Azure OpenAI Responses API multimodal input
 * https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses#multimodal-vision
 */
class ImageUtils @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "ImageUtils"
        private const val DATA_URL_PREFIX = "data:image/jpeg;base64,"
    }

    /**
     * Encodes an image from a URI to a base64 data URL.
     *
     * Reads the image from the given URI, encodes it as base64, and formats it
     * as a data URL suitable for Azure OpenAI Responses API multimodal input.
     *
     * **Process:**
     * 1. Open InputStream from URI via ContentResolver
     * 2. Decode to Bitmap (expects pre-compressed JPEG from PhotoManager)
     * 3. Encode bitmap to base64 string
     * 4. Prepend data URL prefix
     * 5. Recycle bitmap to free memory
     *
     * **Expected Input:**
     * - URI from PhotoManager (Story 2-3) pointing to compressed JPEG
     * - Photos already at 2MP resolution max, 80% JPEG quality
     * - Typical size: 200-500KB
     *
     * **Output:**
     * - Base64 data URL: `data:image/jpeg;base64,{encoded_data}`
     * - Encoded size: ~265-665KB (33% increase over original)
     *
     * @param photoUri URI pointing to the meal photo (content:// or file://)
     * @return Base64-encoded data URL string, or null if encoding fails
     * @throws IOException If URI is inaccessible or file cannot be read
     */
    @Throws(IOException::class)
    fun encodeImageToBase64DataUrl(photoUri: Uri): String? {
        var bitmap: Bitmap? = null
        try {
            // Open input stream from URI
            val inputStream = context.contentResolver.openInputStream(photoUri)
                ?: run {
                    Timber.tag(TAG).e("Failed to open input stream for URI: $photoUri")
                    return null
                }

            // Decode to bitmap
            bitmap = inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: run {
                Timber.tag(TAG).e("Failed to decode bitmap from URI: $photoUri")
                return null
            }

            Timber.tag(TAG).d("Loaded bitmap: ${bitmap.width}x${bitmap.height} from $photoUri")

            // Encode bitmap to base64
            val base64String = encodeBitmapToBase64(bitmap)
                ?: run {
                    Timber.tag(TAG).e("Failed to encode bitmap to base64")
                    return null
                }

            // Return data URL format
            return "$DATA_URL_PREFIX$base64String"
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "IOException while encoding image from URI: $photoUri")
            throw e
        } finally {
            // Recycle bitmap to free memory (critical for avoiding memory leaks)
            bitmap?.recycle()
        }
    }

    /**
     * Encodes a Bitmap to a base64 string (without data URL prefix).
     *
     * Compresses bitmap as JPEG (80% quality to match PhotoManager settings)
     * and encodes the byte array to base64.
     *
     * Note: Caller is responsible for recycling the bitmap after calling this method.
     *
     * @param bitmap The bitmap to encode
     * @return Base64-encoded string, or null if encoding fails
     */
    private fun encodeBitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP) // NO_WRAP for API compatibility

            Timber.tag(TAG).d("Encoded bitmap to base64: ${byteArray.size} bytes → ${base64.length} chars")
            base64
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to encode bitmap to base64")
            null
        }
    }
}
