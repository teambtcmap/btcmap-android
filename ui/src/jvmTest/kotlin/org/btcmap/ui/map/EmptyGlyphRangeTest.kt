package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The empty glyph range is hand-encoded protobuf, so its shape is pinned here:
 * MapLibre parses it as `glyphs { fontstack { name = 1; range = 2; } }` with no
 * glyphs. A malformed message would make MapLibre drop the whole vector tile
 * (maplibre-native#4430) instead of rendering it without labels.
 */
class EmptyGlyphRangeTest {

    @Test
    fun wrapsASingleFontstackWithNoGlyphs() {
        val bytes = emptyGlyphRange(fontstack = "Noto Sans Regular", range = "45824-46079")

        val stacks = readBytesField(bytes, field = 1)
        assertEquals(1, stacks.size)
        val stack = stacks.single()
        assertEquals("Noto Sans Regular", readStringField(stack, field = 1))
        assertEquals("45824-46079", readStringField(stack, field = 2))
        assertEquals(0, readBytesField(stack, field = 3).size)
    }

    @Test
    fun encodesFieldLengthsAboveOneByte() {
        // A 200-byte name forces the length prefix past a single varint byte.
        val fontstack = "x".repeat(200)

        val stack = readBytesField(emptyGlyphRange(fontstack, "0-255"), field = 1).single()

        assertEquals(fontstack, readStringField(stack, field = 1))
        assertEquals("0-255", readStringField(stack, field = 2))
    }
}

private fun readVarint(bytes: ByteArray, start: Int): Pair<Int, Int> {
    var index = start
    var value = 0
    var shift = 0
    while (true) {
        val byte = bytes[index].toInt() and 0xFF
        index++
        value = value or ((byte and 0x7F) shl shift)
        if (byte and 0x80 == 0) return value to index
        shift += 7
    }
}

private fun readBytesField(bytes: ByteArray, field: Int): List<ByteArray> {
    val found = mutableListOf<ByteArray>()
    var index = 0
    while (index < bytes.size) {
        val (tag, afterTag) = readVarint(bytes, index)
        index = afterTag
        val (length, afterLength) = readVarint(bytes, index)
        index = afterLength
        val payload = bytes.copyOfRange(index, index + length)
        index += length
        if (tag shr 3 == field) found += payload
    }
    return found
}

private fun readStringField(bytes: ByteArray, field: Int): String? =
    readBytesField(bytes, field).firstOrNull()?.decodeToString()
