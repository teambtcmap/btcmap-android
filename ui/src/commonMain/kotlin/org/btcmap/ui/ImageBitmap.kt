package org.btcmap.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Decodes encoded image [bytes] (PNG or JPEG) for display, e.g. report evidence. */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap
