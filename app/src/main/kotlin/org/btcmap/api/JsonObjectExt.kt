package org.btcmap.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject

fun JsonObject.string(name: String): String = get(name).asString

fun JsonObject.long(name: String): Long = get(name).asLong

fun JsonObject.double(name: String): Double = get(name).asDouble

fun JsonObject.int(name: String): Int = get(name).asInt

fun JsonObject.obj(name: String): JsonObject = get(name).asJsonObject

fun JsonObject.stringOrNull(name: String): String? = valueOrNull(name)?.asString

fun JsonObject.nonBlankStringOrNull(name: String): String? = stringOrNull(name)?.ifBlank { null }

fun JsonObject.longOrNull(name: String): Long? = valueOrNull(name)?.asLong

fun JsonObject.doubleOrNull(name: String): Double? = valueOrNull(name)?.asDouble

fun JsonObject.objectOrNull(name: String): JsonObject? = valueOrNull(name)?.asJsonObject

fun JsonObject.arrayOrNull(name: String): JsonArray? = valueOrNull(name)?.asJsonArray

private fun JsonObject.valueOrNull(name: String) = get(name)?.takeUnless { it.isJsonNull }
