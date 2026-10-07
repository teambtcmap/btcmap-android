package org.btcmap.i18n

/**
 * The CLDR plural category for [count] in [language], one of `zero`, `one`,
 * `two`, `few`, `many` or `other`. Only the languages the app ships need a rule;
 * the rest fall back to the English one/other split.
 */
internal fun pluralQuantity(language: String, count: Int): String {
    val n = count.coerceAtLeast(0)
    return when (language) {
        "ja", "ko", "th", "vi", "zh" -> "other"

        "ar" -> when {
            n == 0 -> "zero"
            n == 1 -> "one"
            n == 2 -> "two"
            n % 100 in 3..10 -> "few"
            n % 100 in 11..99 -> "many"
            else -> "other"
        }

        "iw" -> when (n) {
            1 -> "one"
            2 -> "two"
            else -> "other"
        }

        // East Slavic: one (…1 but not …11), few (…2–4 but not …12–14), many.
        "ru", "uk" -> when {
            n % 10 == 1 && n % 100 != 11 -> "one"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "few"
            n % 10 == 0 || n % 10 in 5..9 || n % 100 in 11..14 -> "many"
            else -> "other"
        }

        "pl" -> when {
            n == 1 -> "one"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "few"
            else -> "many"
        }

        "cs", "sk" -> when {
            n == 1 -> "one"
            n in 2..4 -> "few"
            else -> "other"
        }

        "ro" -> when {
            n == 1 -> "one"
            n == 0 || n % 100 in 1..19 -> "few"
            else -> "other"
        }

        "sr" -> when {
            n % 10 == 1 && n % 100 != 11 -> "one"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "few"
            else -> "other"
        }

        else -> if (n == 1) "one" else "other"
    }
}
