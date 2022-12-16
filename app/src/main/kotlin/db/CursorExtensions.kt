package db

import android.database.Cursor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.LocalDate
import java.time.ZonedDateTime

fun Cursor.getJsonObject(columnIndex: Int): JsonObject {
    return Json.decodeFromString(getString(columnIndex))
}

fun Cursor.getZonedDateTime(columnIndex: Int): ZonedDateTime? {
    return (getString(columnIndex) ?: "").toZonedDateTime()
}

fun Cursor.getDate(columnIndex: Int): LocalDate {
    return LocalDate.parse(getString(columnIndex))
}

fun Cursor.getBoolean(columnIndex: Int): Boolean {
    return getInt(columnIndex) != 0
}

fun Cursor.getHttpUrl(columnIndex: Int): HttpUrl? {
    return (getString(columnIndex) ?: "").toHttpUrlOrNull()
}

fun Boolean.toSqliteInt(): Int = if (this) 1 else 0

fun String.toZonedDateTime(): ZonedDateTime? {
    return if (isNullOrEmpty()) {
        null
    } else {
        ZonedDateTime.parse(this)
    }
}