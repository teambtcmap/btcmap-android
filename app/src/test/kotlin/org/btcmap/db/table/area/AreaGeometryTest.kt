package org.btcmap.db.table.area

import org.junit.Assert
import org.junit.Test

class AreaGeometryTest {

    @Test
    fun polygon_containsInteriorPoint() {
        val geometry = AreaGeometry.parse(POLYGON)

        Assert.assertTrue(geometry.contains(lat = 48.86, lon = 2.35))
    }

    @Test
    fun polygon_excludesPointOutside() {
        val geometry = AreaGeometry.parse(POLYGON)

        Assert.assertFalse(geometry.contains(lat = 48.0, lon = 2.35))
        Assert.assertFalse(geometry.contains(lat = 48.86, lon = 10.0))
    }

    @Test
    fun polygon_respectsHoles() {
        // A 2x2 square with a 1x1 hole in the middle.
        val geometry = AreaGeometry.parse(
            """{"type":"Polygon","coordinates":[
                [[0,0],[4,0],[4,4],[0,4],[0,0]],
                [[1,1],[3,1],[3,3],[1,3],[1,1]]
            ]}"""
        )

        Assert.assertTrue(geometry.contains(lat = 0.5, lon = 0.5))
        Assert.assertFalse(geometry.contains(lat = 2.0, lon = 2.0))
    }

    @Test
    fun multiPolygon_matchesAnyPart() {
        val geometry = AreaGeometry.parse(
            """{"type":"MultiPolygon","coordinates":[
                [[[0,0],[1,0],[1,1],[0,1],[0,0]]],
                [[[10,10],[11,10],[11,11],[10,11],[10,10]]]
            ]}"""
        )

        Assert.assertTrue(geometry.contains(lat = 0.5, lon = 0.5))
        Assert.assertTrue(geometry.contains(lat = 10.5, lon = 10.5))
        Assert.assertFalse(geometry.contains(lat = 5.0, lon = 5.0))
    }

    @Test
    fun feature_isFlattened() {
        val geometry = AreaGeometry.parse(
            """{"type":"Feature","properties":{},"geometry":$POLYGON}"""
        )

        Assert.assertTrue(geometry.contains(lat = 48.86, lon = 2.35))
    }

    @Test
    fun featureCollection_isFlattened() {
        val geometry = AreaGeometry.parse(
            """{"type":"FeatureCollection","features":[
                {"type":"Feature","properties":{},"geometry":$POLYGON}
            ]}"""
        )

        Assert.assertTrue(geometry.contains(lat = 48.86, lon = 2.35))
    }

    @Test
    fun geometryCollection_isFlattened() {
        val geometry = AreaGeometry.parse(
            """{"type":"GeometryCollection","geometries":[$POLYGON]}"""
        )

        Assert.assertTrue(geometry.contains(lat = 48.86, lon = 2.35))
    }

    @Test
    fun lineString_matchesPointOnTheLine() {
        val geometry = AreaGeometry.parse(
            """{"type":"LineString","coordinates":[[0,0],[2,2],[4,0]]}"""
        )

        Assert.assertTrue(geometry.contains(lat = 1.0, lon = 1.0))
        Assert.assertFalse(geometry.contains(lat = 3.0, lon = 1.0))
    }

    @Test
    fun nullGeoJson_neverMatches() {
        Assert.assertFalse(AreaGeometry.parse(null).contains(lat = 0.0, lon = 0.0))
    }

    @Test
    fun malformedGeoJson_neverMatches() {
        Assert.assertFalse(AreaGeometry.parse("not json").contains(lat = 0.0, lon = 0.0))
    }

    private companion object {
        const val POLYGON =
            """{"type":"Polygon","coordinates":[[[2.22,48.81],[2.47,48.81],[2.47,48.91],[2.22,48.91],[2.22,48.81]]]}"""
    }
}
