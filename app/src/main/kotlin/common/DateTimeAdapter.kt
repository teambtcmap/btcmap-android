package common

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.LocalDateTime

class DateTimeAdapter : TypeAdapter<LocalDateTime>() {
    override fun read(`in`: JsonReader): LocalDateTime? {
        return try {
            when (`in`.peek()) {
                JsonToken.NULL -> {
                    `in`.nextNull()
                    null
                }
                else -> LocalDateTime.parse(`in`.nextString())
            }
        } catch (e: Exception) {
            throw JsonParseException(e)
        }
    }

    override fun write(out: JsonWriter, value: LocalDateTime?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }
}