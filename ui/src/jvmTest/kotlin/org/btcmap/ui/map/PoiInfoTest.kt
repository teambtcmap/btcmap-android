package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry

/**
 * Reading a tapped basemap POI feature into the map's info-sheet model: the
 * name (following the style's own label expression), the OSM class tokens and
 * the point.
 */
class PoiInfoTest {

    private fun feature(json: String): Feature<Geometry, JsonObject?> =
        Feature.fromJsonOrNull<Geometry, JsonObject?>(json)
            ?: error("invalid test feature")

    @Test
    fun readsThePlainNameClassAndPoint() {
        val poi = feature(
            """
            {"type":"Feature",
             "geometry":{"type":"Point","coordinates":[116.4423,39.9341]},
             "properties":{"name":"Harry's Bar","class":"bar","subclass":"bar"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("Harry's Bar", poi?.name)
        assertEquals("bar", poi?.classId)
        assertEquals("bar", poi?.subclass)
        assertEquals(39.9341, poi?.lat)
        assertEquals(116.4423, poi?.lon)
    }

    @Test
    fun prefersLatinAndNonLatinNamesWhereBothExist() {
        val poi = feature(
            """
            {"type":"Feature",
             "geometry":{"type":"Point","coordinates":[116.44,39.93]},
             "properties":{"name:latin":"Holiday Inn","name:nonlatin":"智选假日酒店"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("Holiday Inn · 智选假日酒店", poi?.name)
    }

    @Test
    fun fallsBackToTheEnglishNameThenThePlainName() {
        val english = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,2.0]},
             "properties":{"name":"Bodega","name_en":"Bodega (EN)"}}
            """.trimIndent(),
        ).toPoiInfo("en")
        assertEquals("Bodega (EN)", english?.name)

        val plain = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,2.0]},
             "properties":{"name":"Bodega"}}
            """.trimIndent(),
        ).toPoiInfo("en")
        assertEquals("Bodega", plain?.name)
    }

    @Test
    fun usesThePlainNameRatherThanAShorterLatinName() {
        // The tile's label renders `name_en ?: name`, not `name:latin`, so a
        // short Latin name beside a fuller plain name must not truncate it.
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.44,39.94]},
             "properties":{"name":"JingZun Beijing Duck Restaurant","name:latin":"JingZun",
                           "class":"restaurant","subclass":"restaurant"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("JingZun Beijing Duck Restaurant", poi?.name)
        assertNull(poi?.localName)
    }

    @Test
    fun keepsTheLocalNameWhenItDiffersFromTheLabel() {
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.44,39.94]},
             "properties":{"name":"京尊烤鸭","name_en":"JingZun Roast Duck",
                           "name:latin":"JingZun","class":"restaurant"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("JingZun Roast Duck", poi?.name)
        assertEquals("京尊烤鸭", poi?.localName)
    }

    @Test
    fun showsANonLatinNameThatLivesOnlyInNameZh() {
        // Red Circle Studio's OSM record keeps its Chinese name in name:zh while
        // the label (name_en) is Latin and there is no name:nonlatin.
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.45,39.93]},
             "properties":{"name":"The Scotch Malt Whisky Society (Beijing)",
                           "name:en":"Red Circle Studio","name:zh":"红圈酒吧","class":"bar"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("Red Circle Studio", poi?.name)
        assertEquals("红圈酒吧", poi?.localName)
    }

    @Test
    fun showsTheOriginalNameBesideTheLocalizedOne() {
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.44,39.93]},
             "properties":{"name":"东直门智选假日酒店","name:latin":"Holiday Inn Express",
                           "name:nonlatin":"东直门智选假日酒店","name_en":"Holiday Inn Express",
                           "class":"lodging"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("Holiday Inn Express", poi?.name)
        assertEquals("东直门智选假日酒店", poi?.localName)
    }

    @Test
    fun usesTheNameInTheAppLanguageWhenTheTileHasOne() {
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.44,39.93]},
             "properties":{"name":"东直门智选假日酒店","name:de":"Holiday Inn Frankfurt",
                           "name:nonlatin":"东直门智选假日酒店","class":"lodging"}}
            """.trimIndent(),
        ).toPoiInfo("de")

        assertEquals("Holiday Inn Frankfurt", poi?.name)
        assertEquals("东直门智选假日酒店", poi?.localName)
    }

    @Test
    fun doesNotRepeatANameAlreadyInTheLabel() {
        // No name for the app language, so the label is the fallback and already
        // carries the non-Latin half; it must not be shown a second time.
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[116.44,39.93]},
             "properties":{"name:latin":"Holiday Inn Express",
                           "name:nonlatin":"东直门智选假日酒店","class":"lodging"}}
            """.trimIndent(),
        ).toPoiInfo("en")

        assertEquals("Holiday Inn Express · 东直门智选假日酒店", poi?.name)
        assertNull(poi?.localName)
    }

    @Test
    fun aFeatureWithoutANameIsNotAPoi() {
        val poi = feature(
            """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,2.0]},
             "properties":{"class":"bar"}}
            """.trimIndent(),
        ).toPoiInfo("en")
        assertNull(poi)
    }

    @Test
    fun aFeatureThatIsNotAPointIsNotAPoi() {
        val poi = feature(
            """
            {"type":"Feature",
             "geometry":{"type":"LineString","coordinates":[[0.0,0.0],[1.0,1.0]]},
             "properties":{"name":"A street"}}
            """.trimIndent(),
        ).toPoiInfo("en")
        assertNull(poi)
    }

    @Test
    fun categoryLabelCombinesTheClassTokens() {
        val poi = PoiInfo("X", classId = "lodging", subclass = "hotel", lat = 0.0, lon = 0.0)
        assertEquals("Lodging · Hotel", poi.categoryLabel())
    }

    @Test
    fun categoryLabelSkipsARepeatedClassAndAGenericYes() {
        val repeated = PoiInfo("X", classId = "bar", subclass = "bar", lat = 0.0, lon = 0.0)
        assertEquals("Bar", repeated.categoryLabel())

        val generic = PoiInfo("X", classId = "shop", subclass = "yes", lat = 0.0, lon = 0.0)
        assertEquals("Shop", generic.categoryLabel())

        val none = PoiInfo("X", classId = null, subclass = null, lat = 0.0, lon = 0.0)
        assertNull(none.categoryLabel())
    }

    @Test
    fun categoryTokenPrefersTheSpecificSubclassThenTheClass() {
        val hotel = PoiInfo("X", classId = "lodging", subclass = "hotel", lat = 0.0, lon = 0.0)
        assertEquals("hotel", hotel.categoryToken())

        val repeated = PoiInfo("X", classId = "bar", subclass = "bar", lat = 0.0, lon = 0.0)
        assertEquals("bar", repeated.categoryToken())

        val generic = PoiInfo("X", classId = "shop", subclass = "yes", lat = 0.0, lon = 0.0)
        assertEquals("shop", generic.categoryToken())

        val none = PoiInfo("X", classId = null, subclass = null, lat = 0.0, lon = 0.0)
        assertNull(none.categoryToken())
    }

    @Test
    fun categoryGlyphFollowsTheOsmClass() {
        assertEquals("local_bar", PoiInfo("X", "bar", null, 0.0, 0.0).categoryGlyph())
        assertEquals("hotel", PoiInfo("X", "lodging", null, 0.0, 0.0).categoryGlyph())
        assertEquals("place", PoiInfo("X", "something_else", null, 0.0, 0.0).categoryGlyph())
    }

    @Test
    fun coordinatesLabelRoundsToAboutAMetre() {
        val poi = PoiInfo("X", null, null, lat = 39.934123, lon = 116.442349)
        assertEquals("39.93412, 116.44235", poi.coordinatesLabel())
    }

    @Test
    fun osmLinksPointAtTheFeatureCoordinates() {
        // The basemap tiles carry no OSM element id, so the links target the
        // point, rounded to the same ~1 m as the coordinate label.
        val poi = PoiInfo("X", "bar", "bar", lat = 39.934123, lon = 116.442349)
        assertEquals(
            "https://www.openstreetmap.org/?mlat=39.93412&mlon=116.44235#map=19/39.93412/116.44235",
            poi.osmUrl(),
        )
        assertEquals(
            "https://www.openstreetmap.org/edit#map=19/39.93412/116.44235",
            poi.osmEditUrl(),
        )
    }
}
