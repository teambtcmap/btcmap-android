package org.btcmap.map

import org.btcmap.db.table.area.Area
import org.junit.Assert
import org.junit.Test

class AreaGeoJsonTest {

    private fun area(geoJson: String?) = Area(
        id = 1,
        name = "a",
        type = "community",
        urlAlias = "a",
        icon = null,
        iconWide = null,
        websiteUrl = "https://example.com",
        description = null,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = geoJson,
    )

    @Test
    fun bareGeometry_isWrappedInAFeature() {
        val polygon = """{"coordinates":[[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,0.0]]],"type":"Polygon"}"""

        val result = area(polygon).toRenderGeoJson()

        Assert.assertTrue(result.startsWith("""{"type":"FeatureCollection","features":["""))
        Assert.assertTrue(result.contains(""""geometry":$polygon"""))
    }

    @Test
    fun featureCollection_isPassedThrough() {
        val collection = """{"features":[],"type":"FeatureCollection"}"""

        Assert.assertEquals(collection, area(collection).toRenderGeoJson())
    }

    @Test
    fun feature_isWrappedInACollection() {
        val feature = """{"geometry":{"type":"Polygon","coordinates":[]},"type":"Feature"}"""

        val result = area(feature).toRenderGeoJson()

        Assert.assertTrue(result.contains(""""features":[$feature]"""))
    }

    @Test
    fun missingOrMalformed_becomesEmpty() {
        Assert.assertEquals(EMPTY_GEOJSON, area(null).toRenderGeoJson())
        Assert.assertEquals(EMPTY_GEOJSON, area("not json").toRenderGeoJson())
        Assert.assertEquals(EMPTY_GEOJSON, area("""{"type":"Bogus"}""").toRenderGeoJson())
    }
}
