package org.btcmap.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.roundToInt

// Uploaded photos are capped at 10 MB by the API, so they are downscaled and
// re-encoded before upload, mirroring the Android encoder.
private const val MAX_PHOTO_DIMENSION = 2048
private const val PHOTO_JPEG_QUALITY = 0.85f

/**
 * A file picker for report evidence photos. The dialog must run on the AWT event
 * thread (the Compose UI thread), so it is shown there and the chosen files are
 * read and encoded off it. Returns the photos that decoded as images.
 */
internal fun reportPhotoPicker(window: Frame): suspend () -> List<ByteArray> = {
    val files = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(window, "Choose photos", FileDialog.LOAD).apply {
            isMultipleMode = true
        }
        dialog.isVisible = true
        dialog.files.toList()
    }
    withContext(Dispatchers.IO) { files.mapNotNull(::encodeReportPhoto) }
}

/**
 * Reads [file], downscales it so its longest side is at most
 * [MAX_PHOTO_DIMENSION] and re-encodes it as JPEG, or returns null when it is
 * not a readable image. EXIF orientation is not applied, so a phone photo that
 * was stored sideways can stay sideways.
 */
internal fun encodeReportPhoto(file: File): ByteArray? = runCatching {
    val source = ImageIO.read(file) ?: return@runCatching null

    val longest = maxOf(source.width, source.height)
    val scale = if (longest > MAX_PHOTO_DIMENSION) {
        MAX_PHOTO_DIMENSION.toDouble() / longest
    } else {
        1.0
    }
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)

    // JPEG has no alpha channel, so flatten the image onto white.
    val rgb = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    rgb.createGraphics().apply {
        drawImage(source, 0, 0, width, height, java.awt.Color.WHITE, null)
        dispose()
    }

    ByteArrayOutputStream().use { out ->
        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        try {
            ImageIO.createImageOutputStream(out).use { stream ->
                writer.output = stream
                val params = writer.defaultWriteParam.apply {
                    if (canWriteCompressed()) {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = PHOTO_JPEG_QUALITY
                    }
                }
                writer.write(null, IIOImage(rgb, null, null), params)
            }
        } finally {
            writer.dispose()
        }
        out.toByteArray()
    }
}.getOrNull()
