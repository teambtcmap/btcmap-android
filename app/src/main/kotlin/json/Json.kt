package json

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

fun InputStream.toJsonArray(): List<JSONObject> {
    return JSONArray(this.bufferedReader().use { it.readText() }).toList()
}

fun JSONArray.toList(): List<JSONObject> {
    return (0 until length()).map { getJSONObject(it) }
}

fun JSONArray.toListOfArrays(): List<JSONArray> {
    return (0 until length()).map { getJSONArray(it) }
}