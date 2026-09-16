package org.btcmap.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

fun JsonObject.string(name: String): String = required(name) { it.asString }

fun JsonObject.long(name: String): Long = required(name) { it.asLong }

fun JsonObject.double(name: String): Double = required(name) { it.asDouble }

fun JsonObject.int(name: String): Int = required(name) { it.asInt }

fun JsonObject.obj(name: String): JsonObject = required(name) { it.asJsonObject }

fun JsonObject.stringOrNull(name: String): String? = optional(name) { it.asString }

fun JsonObject.nonBlankStringOrNull(name: String): String? = stringOrNull(name)?.ifBlank { null }

fun JsonObject.longOrNull(name: String): Long? = optional(name) { it.asLong }

fun JsonObject.doubleOrNull(name: String): Double? = optional(name) { it.asDouble }

fun JsonObject.objectOrNull(name: String): JsonObject? = optional(name) { it.asJsonObject }

fun JsonObject.arrayOrNull(name: String): JsonArray? = optional(name) { it.asJsonArray }

fun JsonObject.doubleArrayOrNull(name: String): List<Double>? =
    optional(name) { element -> element.asJsonArray.map { it.asDouble } }

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

private fun JsonObject.valueOrNull(name: String) = get(name)?.takeUnless { it.isJsonNull }

private fun JsonElement.typeName(): String {
    if (isJsonObject) return "object"
    if (isJsonArray) return "array"
    if (isJsonPrimitive) {
        val primitive = this as JsonPrimitive
        return when {
            primitive.isString -> "string"
            primitive.isNumber -> "number"
            primitive.isBoolean -> "boolean"
            else -> "primitive"
        }
    }
    return "null"
}
