package org.btcmap.map

/**
 * Appends [value] as a quoted, escaped JSON string.
 *
 * The marker GeoJSON is built by hand for speed, and a place's icon name comes
 * from the API unchecked: a quote, backslash or control character in it would
 * otherwise corrupt the whole FeatureCollection and hide every marker in the
 * source. Everything that is not a JSON-special character is copied verbatim.
 */
internal fun StringBuilder.appendJsonString(value: String) {
    append('"')
    var segmentStart = 0

    for (index in value.indices) {
        val char = value[index]
        if (char != '"' && char != '\\' && char >= ' ') continue

        append(value, segmentStart, index)
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            else -> {
                append("\\u")
                val hex = char.code.toString(16)
                repeat(4 - hex.length) { append('0') }
                append(hex)
            }
        }
        segmentStart = index + 1
    }

    append(value, segmentStart, value.length)
    append('"')
}
