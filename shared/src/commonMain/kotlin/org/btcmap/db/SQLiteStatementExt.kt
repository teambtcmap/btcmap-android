package org.btcmap.db

import androidx.sqlite.SQLiteStatement
import io.ktor.http.Url
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.btcmap.json.parseJson
import org.btcmap.util.toInstant
import org.btcmap.util.toUrl

fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

fun SQLiteStatement.bindLongOrNull(index: Int, value: Long?) {
    if (value == null) bindNull(index) else bindLong(index, value)
}

fun SQLiteStatement.bindDoubleOrNull(index: Int, value: Double?) {
    if (value == null) bindNull(index) else bindDouble(index, value)
}

fun SQLiteStatement.bindHttpUrlOrNull(index: Int, value: Url?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.bindInstant(index: Int, value: Instant) {
    bindText(index, value.toString())
}

fun SQLiteStatement.bindInstantOrNull(index: Int, value: Instant?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.bindJsonObjectOrNull(index: Int, value: JsonObject?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (isNull(index)) null else getText(index)

fun SQLiteStatement.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

fun SQLiteStatement.getDoubleOrNull(index: Int): Double? =
    if (isNull(index)) null else getDouble(index)

fun SQLiteStatement.getInstant(index: Int): Instant =
    getText(index).toInstant()

fun SQLiteStatement.getInstantOrNull(index: Int): Instant? =
    if (isNull(index)) null else getText(index).toInstant()

fun SQLiteStatement.getHttpUrl(index: Int): Url = getText(index).toUrl()

fun SQLiteStatement.getHttpUrlOrNull(index: Int): Url? =
    if (isNull(index)) null else getText(index).toUrl()

fun SQLiteStatement.getJsonObjectOrNull(index: Int): JsonObject? =
    // A malformed value is treated as absent rather than thrown out of a read
    // (the map and preview screens render straight from these projections).
    if (isNull(index)) null
    else runCatching { parseJson(getText(index)).jsonObject }.getOrNull()