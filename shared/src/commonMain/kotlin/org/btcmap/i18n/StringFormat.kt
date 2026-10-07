package org.btcmap.i18n

/**
 * Substitutes the `%s`/`%d` arguments of an Android-style format string,
 * supporting positional specifiers (`%1$s`), `%%`, and ignored flags and widths.
 * Arguments may be strings, numbers or anything else with a sane `toString`.
 */
internal fun applyArgs(format: String, args: Array<out Any?>): String {
    if (args.isEmpty() || '%' !in format) return format
    val out = StringBuilder(format.length + 16)
    var i = 0
    var next = 0
    while (i < format.length) {
        val c = format[i]
        if (c != '%') {
            out.append(c)
            i++
            continue
        }
        // %% is a literal percent sign.
        if (i + 1 < format.length && format[i + 1] == '%') {
            out.append('%')
            i += 2
            continue
        }
        var j = i + 1
        // Optional positional index, e.g. the "1$" of "%1$s".
        val digitsStart = j
        while (j < format.length && format[j].isDigit()) j++
        var index = -1
        if (j < format.length && format[j] == '$') {
            index = format.substring(digitsStart, j).toInt() - 1
            j++
        } else {
            j = digitsStart
        }
        // Skip flags, width and precision.
        while (j < format.length && format[j] in "-+ #0,.123456789") j++
        if (j >= format.length) break
        // format[j] is the conversion character; the argument is already typed.
        val argIndex = if (index >= 0) index else next++
        out.append(args.getOrNull(argIndex)?.toString().orEmpty())
        i = j + 1
    }
    return out.toString()
}
