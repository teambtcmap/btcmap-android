package users

import db.User
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.regex.Pattern

fun User.lnurl(): String {
    val osmJson: JsonObject = Json.decodeFromString(osm_json)
    val description = osmJson["description"]?.jsonPrimitive?.content ?: ""
    val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(description)
    val matchFound: Boolean = matcher.find()

    return if (matchFound) {
        matcher.group().trim('(', ')')
    } else {
        ""
    }
}

fun User.imgHref(): String {
    val osmJson: JsonObject = Json.decodeFromString(osm_json)
    val img = osmJson["img"]?.jsonObject ?: return ""
    return img["href"]?.jsonPrimitive?.content ?: ""
}