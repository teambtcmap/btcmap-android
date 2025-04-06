package db

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

fun SQLiteStatement.getJsonObjectOld(columnIndex: Int): JSONObject {
    return JSONObject(getText(columnIndex))
}

fun SQLiteStatement.getJsonArray(columnIndex: Int): JSONArray {
    return if (isNull(columnIndex)) {
        JSONArray()
    } else {
        JSONArray(getText(columnIndex))
    }
}

fun SQLiteStatement.getZonedDateTime(columnIndex: Int): ZonedDateTime {
    return getText(columnIndex).toZonedDateTime()!!
}

fun SQLiteStatement.getZonedDateTimeOrNull(columnIndex: Int): ZonedDateTime? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getZonedDateTime(columnIndex)
    }
}

fun SQLiteStatement.getLocalDateOrNull(columnIndex: Int): LocalDate? {
    return if (isNull(columnIndex)) {
        null
    } else {
        LocalDate.parse(getText(columnIndex))
    }
}

fun SQLiteStatement.getText(columnIndex: Int, defaultValue: String): String {
    return if (isNull(columnIndex)) {
        defaultValue
    } else {
        getText(columnIndex)
    }
}

fun SQLiteStatement.getTextOrNull(columnIndex: Int): String? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getText(columnIndex)
    }
}

fun SQLiteStatement.getDate(columnIndex: Int): LocalDate {
    return LocalDate.parse(getText(columnIndex))
}

fun SQLiteStatement.getHttpUrlOrNull(columnIndex: Int): HttpUrl? {
    return (getText(columnIndex, "")).toHttpUrlOrNull()
}

fun String.toZonedDateTime(): ZonedDateTime? {
    return if (isNullOrEmpty()) {
        null
    } else {
        ZonedDateTime.parse(this)
    }
}