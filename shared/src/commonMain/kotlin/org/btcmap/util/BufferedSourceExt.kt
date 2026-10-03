package org.btcmap.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okio.BufferedSource
import org.btcmap.json.parseJson

fun BufferedSource.toJsonArray(): List<JsonObject> {
    val rawJson = readUtf8()
    val jsonArray = parseJson(rawJson).jsonArray

    return List(jsonArray.size) { index ->
        jsonArray[index].jsonObject
    }
}

fun BufferedSource.toJsonObject(): JsonObject = parseJson(readUtf8()).jsonObject

fun BufferedSource.toJsonLongArray(): List<Long> {
    val rawJson = readUtf8()
    val jsonArray = parseJson(rawJson).jsonArray

    return List(jsonArray.size) { jsonArray[it].jsonPrimitive.long }
}
