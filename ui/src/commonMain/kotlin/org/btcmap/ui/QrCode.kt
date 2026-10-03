package org.btcmap.ui

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/**
 * Encodes [text] as a QR code in an [ImageBitmap] of [size] pixels, black on
 * white with a one-module quiet zone.
 *
 * ZXing is a plain JVM dependency, so the encoding lives in common code and the
 * Android and desktop hosts render the same code. The matrix is drawn per
 * module, so a large [size] costs no more than a small one.
 */
fun qrBitmap(text: String, size: Int): ImageBitmap {
    // A 1-pixel request makes ZXing return the matrix at its natural module
    // resolution, which is then scaled up.
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 1, 1, hints)
    val modules = matrix.width
    val cell = size.toFloat() / modules

    val bitmap = ImageBitmap(size, size)
    val canvas = Canvas(bitmap)
    canvas.drawRect(
        left = 0f,
        top = 0f,
        right = size.toFloat(),
        bottom = size.toFloat(),
        paint = Paint().apply { color = Color.White },
    )
    val black = Paint().apply { color = Color.Black }
    for (y in 0 until modules) {
        for (x in 0 until modules) {
            if (matrix.get(x, y)) {
                canvas.drawRect(
                    left = x * cell,
                    top = y * cell,
                    right = (x + 1) * cell,
                    bottom = (y + 1) * cell,
                    paint = black,
                )
            }
        }
    }
    return bitmap
}
