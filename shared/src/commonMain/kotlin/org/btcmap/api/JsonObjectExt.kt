package org.btcmap.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

fun JsonObject.string(name: String): String = required(name) { it.jsonPrimitive.content }

fun JsonObject.long(name: String): Long = required(name) { it.jsonPrimitive.long }

fun JsonObject.double(name: String): Double = required(name) { it.jsonPrimitive.double }

fun JsonObject.int(name: String): Int = required(name) { it.jsonPrimitive.int }

fun JsonObject.boolean(name: String): Boolean = required(name) { it.jsonPrimitive.boolean }

fun JsonObject.obj(name: String): JsonObject = required(name) { it.jsonObject }

fun JsonObject.stringOrNull(name: String): String? = optional(name) { it.jsonPrimitive.content }

fun JsonObject.nonBlankStringOrNull(name: String): String? = stringOrNull(name)?.ifBlank { null }

fun JsonObject.longOrNull(name: String): Long? = optional(name) { it.jsonPrimitive.long }

fun JsonObject.doubleOrNull(name: String): Double? = optional(name) { it.jsonPrimitive.double }

fun JsonObject.objectOrNull(name: String): JsonObject? = optional(name) { it.jsonObject }

fun JsonObject.arrayOrNull(name: String): JsonArray? = optional(name) { it.jsonArray }

fun JsonObject.doubleArrayOrNull(name: String): List<Double>? =
    optional(name) { element -> element.jsonArray.map { it.jsonPrimitive.double } }

private inline fun <T> JsonObject.required(name: String, get: (JsonElement) -> T): T {
    val element = valueOrNull(name)
        ?: throw ApiParseException("Missing required JSON field '$name'")
    return read(name, element, get)
}

private inline fun <T> JsonObject.optional(name: String, get: (JsonElement) -> T): T? {
    val element = valueOrNull(name) ?: return null
    return read(name, element, get)
}

private inline fun <T> read(name: String, element: JsonElement, get: (JsonElement) -> T): T {
    return try {
        get(element)
    } catch (e: ApiParseException) {
        throw e
    } catch (e: RuntimeException) {
        throw ApiParseException(
            "Field '$name' has unexpected type ${element.typeName()}",
            e,
        )
    }
}

private fun JsonObject.valueOrNull(name: String) = get(name)?.takeUnless { it is JsonNull }

private fun JsonElement.typeName(): String = when (this) {
    is JsonObject -> "object"
    is JsonArray -> "array"
    is JsonNull -> "null"
    is JsonPrimitive -> when {
        isString -> "string"
        content == "true" || content == "false" -> "boolean"
        else -> "number"
    }
    else -> "primitive"
}
