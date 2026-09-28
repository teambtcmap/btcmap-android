package org.btcmap.i18n

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * The string at [key], or null when it is missing, blank or not a JSON string.
 *
 * The API fills these maps with strings only, but the cached copy is re-read
 * from disk, so a value that is null, an object or an array is treated as
 * absent instead of letting `JsonElement.asString` return a bogus string or
 * throw.
 */
internal fun JsonObject.stringAt(key: String): String? =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.asString?.ifBlank { null }

/**
 * The string for [locale], falling back to English, or null when neither is
 * present. English is the API's own fallback language.
 */
internal fun JsonObject?.translated(locale: String): String? =
    if (this == null) null else stringAt(locale) ?: stringAt("en")
