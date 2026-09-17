package org.btcmap.comment

import org.btcmap.db.table.comment.Comment
import org.junit.Assert
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CommentExtTest {

    private fun comment(createdAt: String): Comment = Comment(
        id = 7L,
        placeId = 100L,
        comment = "Great coffee!",
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(createdAt),
    )

    private fun expectedDate(createdAt: String, zone: ZoneId): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .format(ZonedDateTime.parse(createdAt).withZoneSameInstant(zone))

    @Test
    fun toAdapterItem_copiesIdAndComment() {
        val item = comment("2024-06-01T10:00:00Z").toAdapterItem(ZoneId.of("UTC"))

        Assert.assertEquals(7L, item.id)
        Assert.assertEquals("Great coffee!", item.comment)
    }

    @Test
    fun toAdapterItem_formatsDateInTheGivenZone() {
        val createdAt = "2024-06-01T23:30:00Z"
        val tokyo = ZoneId.of("Asia/Tokyo")

        val item = comment(createdAt).toAdapterItem(tokyo)

        Assert.assertEquals(expectedDate(createdAt, tokyo), item.localizedDate)
    }

    /**
     * A comment posted shortly before midnight UTC lands on the next day for a
     * user east of UTC; before the zone conversion it kept the UTC date.
     */
    @Test
    fun toAdapterItem_doesNotKeepTheApiZoneDate() {
        val createdAt = "2024-06-01T23:30:00Z"

        val utcDate = comment(createdAt).toAdapterItem(ZoneId.of("UTC")).localizedDate
        val tokyoDate = comment(createdAt).toAdapterItem(ZoneId.of("Asia/Tokyo")).localizedDate

        Assert.assertNotEquals(utcDate, tokyoDate)
    }

    /**
     * A comment posted shortly after midnight UTC lands on the previous day for
     * a user west of UTC; the zone must be applied in both directions.
     */
    @Test
    fun toAdapterItem_formatsTheDateInAZoneWestOfUtc() {
        val createdAt = "2024-06-01T00:30:00Z"
        val losAngeles = ZoneId.of("America/Los_Angeles")

        val item = comment(createdAt).toAdapterItem(losAngeles)

        Assert.assertEquals(expectedDate(createdAt, losAngeles), item.localizedDate)
        Assert.assertNotEquals(
            comment(createdAt).toAdapterItem(ZoneId.of("UTC")).localizedDate,
            item.localizedDate,
        )
    }

    @Test
    fun toAdapterItem_keepsTheDateWhenNoConversionIsNeeded() {
        val createdAt = "2024-06-01T10:00:00Z"

        val item = comment(createdAt).toAdapterItem(ZoneId.of("UTC"))

        Assert.assertEquals(expectedDate(createdAt, ZoneId.of("UTC")), item.localizedDate)
    }
}
