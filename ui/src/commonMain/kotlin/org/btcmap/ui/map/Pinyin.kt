package org.btcmap.ui.map

/**
 * The pinyin reading of each character in [text], one entry per character.
 * Characters that are not Han script — or that the dictionary does not know —
 * map to null.
 *
 * The Android/JVM implementation is jpinyin, which reads the string as a whole
 * so a polyphonic character can use its context (`银行` is `yin hang`).
 */
internal expect fun hanPinyin(text: String): List<String?>

/**
 * The per-character pinyin for [text] when it is mostly Han script, so drawing
 * ruby over it is worthwhile; null when it is not — a Latin name, or a name only
 * partly Han — so the caller renders plain text instead.
 */
internal fun rubyPinyin(text: String): List<String?>? {
    val syllables = hanPinyin(text)
    val han = syllables.count { it != null }
    val visible = text.count { !it.isWhitespace() }
    return if (han >= 2 && han * 2 >= visible) syllables else null
}
