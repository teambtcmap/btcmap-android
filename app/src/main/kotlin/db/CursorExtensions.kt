package db

import android.database.Cursor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

fun Cursor.getJsonObject(columnIndex: Int): JSONObject {
    return JSONObject(getString(columnIndex))
}

fun Cursor.getJsonArray(columnIndex: Int): JSONArray {
    return if (isNull(columnIndex)) {
        JSONArray()
    } else {
        JSONArray(getString(columnIndex))
    }
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