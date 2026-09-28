package org.btcmap.i18n

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.btcmap.db.table.area.Area
import java.util.Locale

/**
 * The area's name in the device language.
 *
 * The fallback mirrors the server's `Area::localized_tag`: the current language
 * wins, then English, then the area's base name. The English step is deliberate
 * and differs from a place's `getLocalizedName`: the single-area endpoint
 * localizes server-side with the same chain, so a cached area and a freshly
 * fetched one read the same.
 */
fun Area.getLocalizedName(): String =
    localizedName.translated(Locale.getDefault().language) ?: name

/** The area's description in the device language, with the same fallback. */
fun Area.getLocalizedDescription(): String? =
    localizedDescription.translated(Locale.getDefault().language) ?: description

private fun JsonObject?.translated(locale: String): String? {
    if (this == null) return null
    return stringAt(locale) ?: stringAt("en")
}

/** The string at [key], or null when it is missing or not a JSON string. */
private fun JsonObject.stringAt(key: String): String? =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.asString?.ifBlank { null }
