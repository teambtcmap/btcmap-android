package element

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class OsmTagsTest {

    @Test
    fun name() {
        val name = "test"
        val tags: OsmTags = OsmTags(mapOf("name" to name))
        assertEquals(name, tags.name(atmLocalizedString = "", unnamedPlaceLocalizedString = ""))
    }

    @Test
    fun nameLocalized() {
        val locale = Locale("it")
        val name = "Shop"
        val nameLocalized = "Negozio"

        val tags: OsmTags = OsmTags(
            mapOf(
                "name" to name,
                "name:${locale.language}" to nameLocalized,
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
        val tags: OsmTags = OsmTags()

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
        val tags: OsmTags = OsmTags(mapOf("amenity" to "atm"))

        assertEquals(
            atmLocalizedString,
            tags.name(
                atmLocalizedString = atmLocalizedString,
                unnamedPlaceLocalizedString = "",
            )
        )
    }
}