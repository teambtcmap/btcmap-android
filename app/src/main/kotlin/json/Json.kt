package json

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

fun InputStream.toJsonArray(): List<JSONObject> {
    return JSONArray(this.bufferedReader().use { it.readText() }).toList()
}

fun InputStream.toJsonObject(): JSONObject {
    return JSONObject(this.bufferedReader().use { it.readText() })
}

fun JSONArray.toList(): List<JSONObject> {
    return (0 until length()).map { getJSONObject(it) }
}