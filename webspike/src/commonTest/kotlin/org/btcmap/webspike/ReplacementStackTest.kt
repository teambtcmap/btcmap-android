package org.btcmap.webspike

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs on the JVM and on wasmJs, so the replacement stack is proven at runtime
 * on the web target, not just compiled.
 */
class ReplacementStackTest {

    @Test
    fun parsesTypedJsonWithKotlinxSerialization() {
        val place = parsePlace(
            """{"id":1,"name":"Satoshi Coffee","updated_at":"2026-10-03T12:00:00"}""",
        )
        assertEquals(1, place.id)
        assertEquals("Satoshi Coffee", place.name)
        assertEquals("2026-10-03T12:00:00", place.updatedAt)
    }

    @Test
    fun readsJsonTreeAndBuildsUrl() {
        assertEquals("Satoshi Coffee", placeName("""{"name":"Satoshi Coffee"}"""))
        assertEquals("https://api.btcmap.org/v4/places", placesUrl("https://api.btcmap.org"))
    }

    @Test
    fun parsesDateAndEncodesBase64ViaOkio() {
        assertEquals(0L, epochSeconds("1970-01-01T00:00:00"))
        assertEquals("aGk=", encode("hi"))
        assertEquals("hi", roundTrip("hi"))
    }
}
