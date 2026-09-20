package org.btcmap.bundle

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Lenient readers shared by the bundled snapshot importers.
 *
 * The snapshot is a committed asset that the bundler validates, but it can also
 * be edited by hand, so display-only values degrade to null instead of aborting
 * the whole seed: a bad optional field must not leave the map empty. Fields
 * that the delta sync depends on (like `updated_at`) are still required and
 * checked by the callers.
 */

/** Reads the current value as a string, mapping an explicit `null` to null. */
internal fun JsonReader.nextStringOrNull(): String? {
    if (peek() == JsonToken.NULL) {
        skipValue()
        return null
    }
    return nextString()
}

/** Reads the current value as a long, mapping an explicit `null` to null. */
internal fun JsonReader.nextLongOrNull(): Long? {
    if (peek() == JsonToken.NULL) {
        skipValue()
        return null
    }
    return nextLong()
}

/**
 * Reads the current value as a JSON object, mapping `null` or a non-object to
 * null.
 */
internal fun JsonReader.nextJsonObjectOrNull(): JsonObject? {
    if (peek() == JsonToken.NULL) {
        skipValue()
        return null
    }
    return JsonParser.parseReader(this).takeIf { it.isJsonObject }?.asJsonObject
}

/**
 * Reads the current value as a JSON array of numbers, mapping `null` to null.
 *
 * The caller decides whether the element count is acceptable; the areas
 * importer expects exactly four (west, south, east, north) and treats any other
 * length as absent.
 */
internal fun JsonReader.nextDoubleListOrNull(): List<Double>? {
    if (peek() == JsonToken.NULL) {
        skipValue()
        return null
    }
    beginArray()
    val values = mutableListOf<Double>()
    while (hasNext()) {
        values.add(nextDouble())
    }
    endArray()
    return values
}

/**
 * Parses an optional bundled timestamp, returning null when it is unparseable.
 *
 * Only display-only timestamps are optional like this; a malformed value must
 * not roll back the whole snapshot and leave the map empty, so it degrades to
 * null exactly like a missing field.
 */
internal fun String.toZonedDateTimeOrNull(): ZonedDateTime? =
    try {
        ZonedDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
