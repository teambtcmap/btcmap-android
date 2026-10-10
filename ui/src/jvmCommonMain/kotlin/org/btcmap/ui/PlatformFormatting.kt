package org.btcmap.ui

import java.text.NumberFormat
import java.util.Locale
import org.btcmap.platform.currentLocale

/**
 * The locale-aware number format both hosts pass to [appLabels], so their
 * grouping and decimal separators match the current locale. Kept out of
 * commonMain because it uses `java.text`/`java.util`.
 */
fun defaultFormatNumber(value: Double, maximumFractionDigits: Int): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag(currentLocale())).apply {
        this.maximumFractionDigits = maximumFractionDigits
    }.format(value)
