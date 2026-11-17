package org.btcmap.map

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.json.parseJson
import org.junit.Assert
import org.junit.Test

class FeatureCollectionGeoJsonTest {

    @Test
    fun mergeFeatureCollections_keepsEveryFeatureInOrder() {
        val first = collection(1)
        val second = collection(2)

        val merged = mergeFeatureCollections(listOf(first, EMPTY_GEOJSON, second))

        val features = parseJson(merged).jsonObject
            .getValue("features").jsonArray
        Assert.assertEquals(2, features.size)
        Assert.assertEquals(
            1,
            features[0].jsonObject
                .getValue("properties").jsonObject
                .getValue("id").jsonPrimitive.content.toInt(),
        )
        Assert.assertEquals(
            2,
            features[1].jsonObject
                .getValue("properties").jsonObject
                .getValue("id").jsonPrimitive.content.toInt(),
        )
    }

    @Test
    fun mergeFeatureCollections_returnsEmptyCollectionForNoFeatures() {
        Assert.assertEquals(EMPTY_GEOJSON, mergeFeatureCollections(emptyList()))
        Assert.assertEquals(EMPTY_GEOJSON, mergeFeatureCollections(listOf(EMPTY_GEOJSON)))
        Assert.assertEquals(EMPTY_GEOJSON, mergeFeatureCollections(listOf("not json")))
    }

    private fun collection(id: Int): String =
        """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"id":$id}}]}"""
}
