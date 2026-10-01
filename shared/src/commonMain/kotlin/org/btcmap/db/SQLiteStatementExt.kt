package org.btcmap.db

import androidx.sqlite.SQLiteStatement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.ZonedDateTime

fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

fun SQLiteStatement.bindLongOrNull(index: Int, value: Long?) {
    if (value == null) bindNull(index) else bindLong(index, value)
}

fun SQLiteStatement.bindDoubleOrNull(index: Int, value: Double?) {
    if (value == null) bindNull(index) else bindDouble(index, value)
}

fun SQLiteStatement.bindHttpUrlOrNull(index: Int, value: HttpUrl?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.bindZonedDateTime(index: Int, value: ZonedDateTime) {
    bindText(index, value.toString())
}

fun SQLiteStatement.bindZonedDateTimeOrNull(index: Int, value: ZonedDateTime?) {
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

fun SQLiteStatement.getZonedDateTime(index: Int): ZonedDateTime =
    ZonedDateTime.parse(getText(index))

fun SQLiteStatement.getZonedDateTimeOrNull(index: Int): ZonedDateTime? =
    if (isNull(index)) null else ZonedDateTime.parse(getText(index))

fun SQLiteStatement.getHttpUrl(index: Int): HttpUrl = getText(index).toHttpUrl()

fun SQLiteStatement.getHttpUrlOrNull(index: Int): HttpUrl? =
    if (isNull(index)) null else getText(index).toHttpUrl()

fun SQLiteStatement.getJsonObjectOrNull(index: Int): JsonObject? =
    // A malformed value is treated as absent rather than thrown out of a read
    // (the map and preview screens render straight from these projections).
    if (isNull(index)) null
    else runCatching { JsonParser.parseString(getText(index)).asJsonObject }.getOrNull()