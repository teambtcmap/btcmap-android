package json

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

fun InputStream.toJsonArray(): List<JSONObject> {
    val rawJson = this.bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(rawJson)

    return List(jsonArray.length()) { index ->
        jsonArray.getJSONObject(index)
    }
}

fun InputStream.toJsonObject(): JSONObject {
    val rawJson = this.bufferedReader().use { it.readText() }
    return JSONObject(rawJson)
}