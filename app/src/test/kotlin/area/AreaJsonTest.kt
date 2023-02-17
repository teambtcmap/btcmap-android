package area

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaJsonTest {

    @Test
    fun valid() {
        assertEquals(
            true,
            AreaJson(
                id = "",
                tags = JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("test"),
                        "geo_json" to JsonObject(emptyMap()),
                    ),
                ),
                created_at = "",
                updated_at = "",
                deleted_at = "",
            ).valid(),
        )

        assertEquals(
            false,
            AreaJson(
                id = "",
                tags = JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("test"),
                        "box:north" to JsonPrimitive(10),
                        "box:south" to JsonPrimitive(5),
                        "box:west" to JsonPrimitive(5),
                        "box:east" to JsonPrimitive(10),
                    ),
                ),
                created_at = "",
                updated_at = "",
                deleted_at = "",
            ).valid(),
        )
    }
}