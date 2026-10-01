package org.btcmap.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// Report evidence is capped at 10 MB per photo by the API, and a camera shot
// can easily exceed that, so photos are downscaled and re-encoded before they
// are uploaded.
private const val MAX_EVIDENCE_DIMENSION = 2048
private const val EVIDENCE_JPEG_QUALITY = 85

/**
 * Reads the image at [uri], downscales it so its longest side is at most
 * [MAX_EVIDENCE_DIMENSION] pixels and re-encodes it as JPEG for use as report
 * evidence. [ImageDecoder] applies the source's EXIF orientation, so the result
 * is upright regardless of how the camera stored it.
 */
suspend fun Context.encodeEvidencePhoto(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val source = ImageDecoder.createSource(contentResolver, uri)

    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        // compress() needs a software-backed bitmap; ImageDecoder may otherwise
        // hand back a hardware one.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > MAX_EVIDENCE_DIMENSION) {
            val scale = MAX_EVIDENCE_DIMENSION.toDouble() / longestSide
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt(),
                (info.size.height * scale).roundToInt(),
            )
        }
    }

    try {
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, EVIDENCE_JPEG_QUALITY, out)
            out.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
