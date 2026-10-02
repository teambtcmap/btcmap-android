package org.btcmap.desktop

import androidx.compose.ui.graphics.toPixelMap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlin.test.Test
import kotlin.test.assertEquals

/** The QR the desktop draws for a Lightning invoice must scan back to it. */
class QrBitmapTest {

    private val invoice = "lnbc1u1p3exampleinvoice"

    @Test
    fun encodesScannableQr() {
        val size = 256
        val pixels = generateQrBitmap(invoice, size).toPixelMap().let { map ->
            IntArray(size * size) { index ->
                val color = map[index % size, index / size]
                (((color.red * 255).toInt()) shl 16) or
                    (((color.green * 255).toInt()) shl 8) or
                    ((color.blue * 255).toInt())
            }
        }

        val result = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels))),
        )

        assertEquals(invoice, result.text)
    }
}
