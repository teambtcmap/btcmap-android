package org.btcmap.comment

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.btcmap.db.table.comment.Comment
import org.btcmap.platform.currentLanguage
import org.junit.Assert
import org.junit.Test
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CommentExtTest {

    private fun comment(createdAt: String): Comment = Comment(
        id = 7L,
        placeId = 100L,
        comment = "Great coffee!",
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt),
    )

    private fun expectedDate(
        createdAt: String,
        timeZone: String,
        language: String = currentLanguage(),
    ): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.forLanguageTag(language))
            .withZone(ZoneId.of(timeZone))
            .format(java.time.Instant.ofEpochSecond(Instant.parse(createdAt).epochSeconds))

    @Test
    fun toAdapterItem_copiesIdAndComment() {
        val item = comment("2024-06-01T10:00:00Z").toAdapterItem(TimeZone.of("UTC"))

        Assert.assertEquals(7L, item.id)
        Assert.assertEquals("Great coffee!", item.comment)
    }

    @Test
    fun toAdapterItem_formatsDateInTheGivenZone() {
        val createdAt = "2024-06-01T23:30:00Z"
        val tokyo = TimeZone.of("Asia/Tokyo")

        val item = comment(createdAt).toAdapterItem(tokyo)

        Assert.assertEquals(expectedDate(createdAt, tokyo.id), item.localizedDate)
    }

    /**
     * A comment posted shortly before midnight UTC lands on the next day for a
     * user east of UTC; before the zone conversion it kept the UTC date.
     */
    @Test
    fun toAdapterItem_doesNotKeepTheApiZoneDate() {
        val createdAt = "2024-06-01T23:30:00Z"

        val utcDate = comment(createdAt).toAdapterItem(TimeZone.of("UTC")).localizedDate
        val tokyoDate = comment(createdAt).toAdapterItem(TimeZone.of("Asia/Tokyo")).localizedDate

        Assert.assertNotEquals(utcDate, tokyoDate)
    }

    /**
     * A comment posted shortly after midnight UTC lands on the previous day for
     * a user west of UTC; the zone must be applied in both directions.
     */
    @Test
    fun toAdapterItem_formatsTheDateInAZoneWestOfUtc() {
        val createdAt = "2024-06-01T00:30:00Z"
        val losAngeles = TimeZone.of("America/Los_Angeles")

        val item = comment(createdAt).toAdapterItem(losAngeles)

        Assert.assertEquals(expectedDate(createdAt, losAngeles.id), item.localizedDate)
        Assert.assertNotEquals(
            comment(createdAt).toAdapterItem(TimeZone.of("UTC")).localizedDate,
            item.localizedDate,
        )
    }

    @Test
    fun toAdapterItem_keepsTheDateWhenNoConversionIsNeeded() {
        val createdAt = "2024-06-01T10:00:00Z"

        val item = comment(createdAt).toAdapterItem(TimeZone.of("UTC"))

        Assert.assertEquals(expectedDate(createdAt, "UTC"), item.localizedDate)
    }

    /**
     * The formatter is rebuilt per call so it follows the language it is given
     * (the device language in production) instead of the one active at startup.
     */
    @Test
    fun toAdapterItem_formatsTheDateInTheGivenLocale() {
        val createdAt = "2024-06-01T10:00:00Z"
        val utc = TimeZone.of("UTC")

        val us = comment(createdAt).toAdapterItem(utc, "en-US")
        val germany = comment(createdAt).toAdapterItem(utc, "de-DE")

        Assert.assertEquals(expectedDate(createdAt, utc.id, "en-US"), us.localizedDate)
        Assert.assertEquals(expectedDate(createdAt, utc.id, "de-DE"), germany.localizedDate)
        Assert.assertNotEquals(us.localizedDate, germany.localizedDate)
    }
}
