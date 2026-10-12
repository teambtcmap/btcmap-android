package org.btcmap.map

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert
import org.junit.Test

/** The beta update dialog's build stamps. */
class BuildStampTest {

    @Test
    fun buildStamp_rendersTheBuildTime() {
        Assert.assertEquals(
            "2026-10-12 13:22",
            buildStamp(
                buildTime = Instant.parse("2026-10-12T13:22:00Z").toEpochMilli(),
                versionName = "1.2.0-beta",
                zone = ZoneOffset.UTC,
            ),
        )
    }

    @Test
    fun buildStamp_rendersInTheGivenZone() {
        Assert.assertEquals(
            "2026-10-12 15:22",
            buildStamp(
                buildTime = Instant.parse("2026-10-12T13:22:00Z").toEpochMilli(),
                versionName = "1.2.0-beta",
                zone = ZoneOffset.ofHours(2),
            ),
        )
    }

    @Test
    fun buildStamp_fallsBackToTheVersionNameWhenUnstamped() {
        Assert.assertEquals(
            "1.2.0-beta",
            buildStamp(buildTime = 0L, versionName = "1.2.0-beta", zone = ZoneOffset.UTC),
        )
    }
}
