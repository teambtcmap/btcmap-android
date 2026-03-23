package org.btcmap.db

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.time.ZonedDateTime

fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

fun SQLiteStatement.bindLongOrNull(index: Int, value: Long?) {
    if (value == null) bindNull(index) else bindLong(index, value)
}

fun SQLiteStatement.bindHttpUrlOrNull(index: Int, value: HttpUrl?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.bindZonedDateTimeOrNull(index: Int, value: ZonedDateTime?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.bindJsonObjectOrNull(index: Int, value: JSONObject?) {
    if (value == null) bindNull(index) else bindText(index, value.toString())
}

fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (isNull(index)) null else getText(index)

fun SQLiteStatement.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

fun SQLiteStatement.getZonedDateTimeOrNull(index: Int): ZonedDateTime? =
    if (isNull(index)) null else ZonedDateTime.parse(getText(index))

fun SQLiteStatement.getHttpUrlOrNull(index: Int): HttpUrl? =
    if (isNull(index)) null else getText(index).toHttpUrl()

fun SQLiteStatement.getJsonObjectOrNull(index: Int): JSONObject? =
    if (isNull(index)) null else JSONObject(getText(index))