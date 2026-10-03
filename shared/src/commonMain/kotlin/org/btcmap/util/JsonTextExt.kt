package org.btcmap.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.btcmap.json.parseJson

fun String.toJsonArray(): List<JsonObject> {
    val jsonArray = parseJson(this).jsonArray

    return List(jsonArray.size) { index ->
        jsonArray[index].jsonObject
    }
}

fun String.toJsonObject(): JsonObject = parseJson(this).jsonObject

fun String.toJsonLongArray(): List<Long> {
    val jsonArray = parseJson(this).jsonArray

    return List(jsonArray.size) { jsonArray[it].jsonPrimitive.long }
}
