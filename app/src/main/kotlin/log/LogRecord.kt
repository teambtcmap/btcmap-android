package log

import org.json.JSONObject

data class LogRecord(
    val id: Long,
    val tags: JSONObject,
    val createdAt: String,
)
