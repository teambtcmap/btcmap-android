package org.btcmap.json

import com.google.gson.JsonObject
import java.io.InputStream

fun InputStream.toJsonArray(): List<JsonObject> {
    val rawJson = this.bufferedReader().use { it.readText() }
    val jsonArray = com.google.gson.JsonParser.parseString(rawJson).asJsonArray

    return List(jsonArray.size()) { index ->
        jsonArray.get(index).asJsonObject
    }
}

fun InputStream.toJsonObject(): JsonObject {
    val rawJson = this.bufferedReader().use { it.readText() }
    return com.google.gson.JsonParser.parseString(rawJson).asJsonObject
}