package db

import androidx.sqlite.SQLiteStatement
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

fun SQLiteStatement.getJsonObject(columnIndex: Int): JsonObject {
    return Json.parseToJsonElement(getText(columnIndex)).jsonObject
}

fun SQLiteStatement.getInstant(columnIndex: Int): Instant {
    return Instant.parse(getText(columnIndex))
}