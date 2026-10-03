package org.btcmap.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.ByteArrayInputStream

@Suppress("DEPRECATION")
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap =
    loadImageBitmap(ByteArrayInputStream(bytes))
