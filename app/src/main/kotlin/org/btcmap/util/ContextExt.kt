package org.btcmap.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// Uploaded photos are capped at 10 MB by the API, and a camera shot can easily
// exceed that, so photos are downscaled and re-encoded before they are uploaded.
private const val MAX_PHOTO_DIMENSION = 2048
private const val PHOTO_JPEG_QUALITY = 85

/**
 * Reads the image at [uri], downscales it so its longest side is at most
 * [MAX_PHOTO_DIMENSION] pixels and re-encodes it as JPEG for upload.
 * [ImageDecoder] applies the source's EXIF orientation, so the result is
 * upright regardless of how the camera stored it.
 */
suspend fun Context.encodePhoto(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val source = ImageDecoder.createSource(contentResolver, uri)

    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        // compress() needs a software-backed bitmap; ImageDecoder may otherwise
        // hand back a hardware one.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > MAX_PHOTO_DIMENSION) {
            val scale = MAX_PHOTO_DIMENSION.toDouble() / longestSide
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt(),
                (info.size.height * scale).roundToInt(),
            )
        }
    }

    try {
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, out)
            out.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
