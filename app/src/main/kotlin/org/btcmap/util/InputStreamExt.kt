package org.btcmap.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStream

fun InputStream.toJsonArray(): List<JsonObject> {
    val rawJson = this.bufferedReader().use { it.readText() }
    val jsonArray = JsonParser.parseString(rawJson).asJsonArray

    return List(jsonArray.size()) { index ->
        jsonArray.get(index).asJsonObject
    }
}

fun InputStream.toJsonObject(): JsonObject {
    val rawJson = this.bufferedReader().use { it.readText() }
    return JsonParser.parseString(rawJson).asJsonObject
}