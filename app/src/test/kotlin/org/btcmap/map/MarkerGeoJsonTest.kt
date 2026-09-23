package org.btcmap.map

import org.btcmap.db.table.place.Marker
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class MarkerGeoJsonTest {
    private val now = ZonedDateTime.parse("2026-06-01T00:00:00Z")

    @Test
    fun isBoosted_trueForFutureBoost() {
        val marker = marker(boostedUntil = ZonedDateTime.parse("2026-06-02T00:00:00Z"))

        Assert.assertTrue(marker.isBoosted(now))
    }

    @Test
    fun isBoosted_falseForExpiredBoost() {
        val marker = marker(boostedUntil = ZonedDateTime.parse("2026-05-31T23:59:59Z"))

        Assert.assertFalse(marker.isBoosted(now))
    }

    @Test
    fun isBoosted_falseWhenBoostIsNull() {
        val marker = marker(boostedUntil = null)

        Assert.assertFalse(marker.isBoosted(now))
    }

    @Test
    fun markerImageName_usesBoostedVariantOnlyWhileActive() {
        val active = marker(boostedUntil = ZonedDateTime.parse("2026-06-02T00:00:00Z"))
        val expired = marker(boostedUntil = ZonedDateTime.parse("2026-05-31T23:59:59Z"))

        Assert.assertTrue(active.markerImageName(now).endsWith("-boosted"))
        Assert.assertFalse(expired.markerImageName(now).contains("-boosted"))
    }

    @Test
    fun toMarkerGeoJson_marksExpiredBoostAsNotBoosted() {
        val json = listOf(marker(boostedUntil = ZonedDateTime.parse("2026-05-31T23:59:59Z")))
            .toMarkerGeoJson(now)

        Assert.assertTrue(json.contains("\"boosted\":false"))
    }

    @Test
    fun isOutdated_trueWhenNeverVerified() {
        val marker = marker(boostedUntil = null, verifiedAt = null)

        Assert.assertTrue(marker.isOutdated(now))
    }

    @Test
    fun isOutdated_trueWhenVerifiedOverAYearAgo() {
        val marker = marker(
            boostedUntil = null,
            verifiedAt = ZonedDateTime.parse("2025-05-31T00:00:00Z"),
        )

        Assert.assertTrue(marker.isOutdated(now))
    }

    @Test
    fun isOutdated_falseWhenVerifiedWithinTheLastYear() {
        val marker = marker(
            boostedUntil = null,
            verifiedAt = ZonedDateTime.parse("2026-05-01T00:00:00Z"),
        )

        Assert.assertFalse(marker.isOutdated(now))
    }

    private fun marker(
        boostedUntil: ZonedDateTime?,
        verifiedAt: ZonedDateTime? = ZonedDateTime.parse("2026-05-01T00:00:00Z"),
    ): Marker {
        return Marker(
            id = 1,
            lat = 0.0,
            lon = 0.0,
            icon = "local_cafe",
            boostedUntil = boostedUntil,
            requiredAppUrl = null,
            comments = 0,
            verifiedAt = verifiedAt,
        )
    }
}
