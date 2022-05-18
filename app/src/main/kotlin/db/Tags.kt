package db

import org.json.JSONObject

class Tags private constructor(
    private val rawJson: String,
    private val tags: JSONObject
) {
    constructor(rawJson: String) : this(rawJson, JSONObject(rawJson))
    constructor(jsonObject: JSONObject) : this(jsonObject.toString(), jsonObject)

    fun toFormattedJson(): String = tags.toString(4)
    override fun toString(): String = rawJson
    operator fun contains(key: String): Boolean = tags.has(key)
    operator fun get(key: String): String? = if (tags.has(key)) tags.getString(key) else null
}