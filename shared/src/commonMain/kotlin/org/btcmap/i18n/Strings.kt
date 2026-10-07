package org.btcmap.i18n

import org.btcmap.platform.currentLocale

/**
 * The app's UI strings, resolved by locale with an English fallback, shared by
 * the Android and desktop hosts so both render the same text.
 *
 * Keys are the names the strings had as Android resources (see the `tables`
 * package). Plurals are stored under `<key>#<quantity>` and read with [plural].
 */
class Strings private constructor(
    private val language: String,
    private val table: Map<String, String>,
) {
    /** [key] for this locale, falling back to English and then to the key. */
    operator fun get(key: String): String = table[key] ?: ENGLISH[key] ?: key

    /** [key] with its `%1$s`/`%2$d` arguments substituted. */
    fun format(key: String, vararg args: Any?): String = applyArgs(get(key), args)

    /** The plural of [key] for [count], chosen with this locale's plural rule. */
    fun plural(key: String, count: Int): String {
        val quantity = pluralQuantity(language, count)
        val template = table["$key#$quantity"]
            ?: table["$key#other"]
            ?: table["$key#one"]
            ?: ENGLISH["$key#$quantity"]
            ?: ENGLISH["$key#other"]
            ?: return key
        return applyArgs(template, arrayOf(count))
    }

    companion object {
        private val ENGLISH: Map<String, String> = tableForExact("en")!!

        /** The catalog for [locale], which may carry a region such as `pt-BR`. */
        fun forLocale(locale: String): Strings {
            val tag = locale.replace('_', '-')
            val language = tag.substringBefore('-')
            val table = tableForExact(tag) ?: tableForExact(language) ?: ENGLISH
            return Strings(language, table)
        }

        /** The catalog for the device locale. */
        fun current(): Strings = forLocale(currentLocale())
    }
}
