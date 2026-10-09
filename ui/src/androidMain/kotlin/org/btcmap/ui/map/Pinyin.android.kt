package org.btcmap.ui.map

import com.github.stuxuhai.jpinyin.PinyinFormat
import com.github.stuxuhai.jpinyin.PinyinHelper

/**
 * [hanPinyin] backed by jpinyin on the Android host.
 *
 * jpinyin reads a whole string so a polyphonic character can use its context
 * (`银行` is `yin hang`, not `yin xing`), but it also merges a dictionary word
 * into a single token (`智选` becomes `zhixuan`), which would break the ruby
 * alignment. So the phrase-aware reading is used only when it yields exactly one
 * token per character; otherwise each character is read on its own, which always
 * aligns. A character that is not Han, or that has no reading, maps to null.
 */
internal actual fun hanPinyin(text: String): List<String?> {
    val chars = text.toList()
    val phrase = runCatching {
        PinyinHelper.convertToPinyinString(text, SEPARATOR, PinyinFormat.WITHOUT_TONE)
    }.getOrNull()
    val tokens = phrase?.split(SEPARATOR)
    if (tokens != null && tokens.size == chars.size) {
        return chars.mapIndexed { index, char -> tokens[index].asPinyin(char) }
    }
    return chars.map(::pinyinOf)
}

private fun pinyinOf(char: Char): String? =
    PinyinHelper.convertToPinyinArray(char, PinyinFormat.WITHOUT_TONE)
        ?.firstOrNull()
        ?.asPinyin(char)

/** jpinyin echoes a non-Han character back as itself; that is not a reading. */
private fun String.asPinyin(char: Char): String? =
    takeIf { it != char.toString() && it.isNotBlank() }

/** A separator that cannot occur in a name, so the split keeps one token per character. */
private const val SEPARATOR = "\u0001"
