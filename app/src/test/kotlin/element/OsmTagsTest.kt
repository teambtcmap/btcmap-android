package element

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class OsmTagsTest {

    @Test
    fun name() {
        val name = "test"
        val tags: OsmTags = OsmTags(mapOf("name" to JsonPrimitive(name)))
        assertEquals(name, tags.name(atmLocalizedString = "", unnamedPlaceLocalizedString = ""))
    }

    @Test
    fun nameLocalized() {
        val locale = Locale("it")
        val name = "Shop"
        val nameLocalized = "Negozio"

        val tags: OsmTags = OsmTags(
            mapOf(
                "name" to JsonPrimitive(name),
                "name:${locale.language}" to JsonPrimitive(nameLocalized)
            )
        )

        assertEquals(
            nameLocalized,
            tags.name(atmLocalizedString = "", unnamedPlaceLocalizedString = "", locale = locale)
        )
    }

    @Test
    fun nameEmpty() {
        val unnamedPlaceLocalizedString = "Unnamed"
        val tags: OsmTags = OsmTags(emptyMap())

        assertEquals(
            unnamedPlaceLocalizedString,
            tags.name(
                atmLocalizedString = "",
                unnamedPlaceLocalizedString = unnamedPlaceLocalizedString,
            )
        )
    }

    @Test
    fun nameAtm() {
        val atmLocalizedString = "ATM"
        val tags: OsmTags = OsmTags(mapOf("amenity" to JsonPrimitive("atm")))

        assertEquals(
            atmLocalizedString,
            tags.name(
                atmLocalizedString = atmLocalizedString,
                unnamedPlaceLocalizedString = "",
            )
        )
    }
}