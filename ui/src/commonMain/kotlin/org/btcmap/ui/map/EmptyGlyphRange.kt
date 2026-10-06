package org.btcmap.ui.map

/**
 * A glyph-range PBF with no glyphs for [fontstack]/[range], matching the format
 * MapLibre's glyph endpoint serves:
 *
 * ```
 * glyphs    { repeated fontstack stacks = 1; }
 * fontstack { required string name = 1; required string range = 2; repeated glyph glyphs = 3; }
 * ```
 *
 * The bundled glyph set omits the CJK and Hangul ranges, and a vector tile whose
 * glyph request errors is dropped whole by MapLibre Native (maplibre-native#4430),
 * blanking the basemap. Serving a well-formed empty range instead lets the tile
 * render with its non-CJK labels. See `BundledMapResources.kt`.
 */
internal fun emptyGlyphRange(fontstack: String, range: String): ByteArray {
    val fontstackMessage = ProtoBuilder()
        .writeStringField(field = 1, fontstack)
        .writeStringField(field = 2, range)
        .toByteArray()
    return ProtoBuilder()
        .writeBytesField(field = 1, fontstackMessage)
        .toByteArray()
}

/** A minimal protobuf writer for length-delimited fields. */
private class ProtoBuilder {
    private val bytes = ArrayList<Byte>()

    fun writeStringField(field: Int, value: String): ProtoBuilder =
        writeBytesField(field, value.encodeToByteArray())

    fun writeBytesField(field: Int, value: ByteArray): ProtoBuilder {
        writeVarint((field shl 3) or WIRE_TYPE_LENGTH_DELIMITED)
        writeVarint(value.size)
        value.forEach { bytes.add(it) }
        return this
    }

    fun toByteArray(): ByteArray = ByteArray(bytes.size) { bytes[it] }

    private fun writeVarint(value: Int) {
        var remaining = value
        while (remaining and 0x7F.inv() != 0) {
            bytes.add(((remaining and 0x7F) or 0x80).toByte())
            remaining = remaining ushr 7
        }
        bytes.add(remaining.toByte())
    }

    private companion object {
        const val WIRE_TYPE_LENGTH_DELIMITED = 2
    }
}
