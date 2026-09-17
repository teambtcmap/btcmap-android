package org.btcmap.bundle

import com.google.gson.stream.JsonReader
import org.junit.Assert
import org.junit.Test
import java.io.StringReader
import java.time.ZonedDateTime

class BundledPlacesTest {
    private fun reader(json: String) = JsonReader(StringReader(json))

    @Test
    fun readBundledPlace_parsesSeededFields() {
        val json = """
            {
              "id": 42,
              "lat": 1.5,
              "lon": 2.5,
              "icon": "local_cafe",
              "name": "Cafe",
              "comments": 7,
              "boosted_until": "2026-02-01T00:00:00Z",
              "unknown": "ignored"
            }
        """.trimIndent()

        val place = reader(json).readBundledPlace()

        Assert.assertEquals(42L, place.id)
        Assert.assertEquals(1.5, place.lat, 0.0)
        Assert.assertEquals(2.5, place.lon, 0.0)
        Assert.assertEquals("local_cafe", place.icon)
        Assert.assertEquals("Cafe", place.name)
        Assert.assertEquals(7L, place.comments)
        Assert.assertEquals(ZonedDateTime.parse("2026-02-01T00:00:00Z"), place.boostedUntil)
        Assert.assertTrue(place.bundled)
        Assert.assertEquals(ZonedDateTime.parse("2000-01-01T00:00:00Z"), place.updatedAt)
    }

    @Test
    fun readBundledPlace_acceptsExplicitNulls() {
        val json = """
            {
              "id": 1,
              "lat": 0.0,
              "lon": 0.0,
              "icon": "store",
              "name": null,
              "comments": null,
              "boosted_until": null
            }
        """.trimIndent()

        val place = reader(json).readBundledPlace()

        Assert.assertNull(place.name)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
    }

    @Test
    fun readBundledPlace_acceptsMissingOptionalFields() {
        val json = """{"id":1,"lat":0.0,"lon":0.0,"icon":"store"}"""

        val place = reader(json).readBundledPlace()

        Assert.assertNull(place.name)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
    }

    @Test
    fun readBundledPlace_rejectsMissingRequiredFields() {
        val cases = mapOf(
            "id" to """{"lat":0.0,"lon":0.0,"icon":"store"}""",
            "lat" to """{"id":1,"lon":0.0,"icon":"store"}""",
            "lon" to """{"id":1,"lat":0.0,"icon":"store"}""",
            "icon" to """{"id":1,"lat":0.0,"lon":0.0}""",
        )

        cases.forEach { (field, json) ->
            try {
                reader(json).readBundledPlace()
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }
}
