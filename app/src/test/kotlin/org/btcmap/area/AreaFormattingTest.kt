package org.btcmap.area

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.R
import org.btcmap.api.GetEventsItem
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class AreaFormattingTest {

    @Test
    fun descriptionParagraphs_nullDescription_returnsEmptyList() {
        Assert.assertTrue(descriptionParagraphs(null).isEmpty())
    }

    @Test
    fun descriptionParagraphs_blankDescription_returnsEmptyList() {
        Assert.assertTrue(descriptionParagraphs("   \n\n  ").isEmpty())
    }

    @Test
    fun descriptionParagraphs_singleParagraph_returnsItTrimmed() {
        Assert.assertEquals(
            listOf("Greater Paris"),
            descriptionParagraphs("  Greater Paris  "),
        )
    }

    @Test
    fun descriptionParagraphs_splitsOnBlankLines() {
        Assert.assertEquals(
            listOf("First", "Second", "Third"),
            descriptionParagraphs("First\n\nSecond\n\nThird"),
        )
    }

    @Test
    fun descriptionParagraphs_toleratesWhitespaceAroundSeparator() {
        Assert.assertEquals(
            listOf("First", "Second"),
            descriptionParagraphs("First\n   \nSecond"),
        )
    }

    @Test
    fun descriptionParagraphs_keepsSingleNewlines() {
        Assert.assertEquals(
            listOf("First\nSecond"),
            descriptionParagraphs("First\nSecond"),
        )
    }

    @Test
    fun websiteDisplayText_stripsSchemeAndTrailingSlash() {
        Assert.assertEquals(
            "btcmap.org/community/grand-paris",
            websiteDisplayText("https://btcmap.org/community/grand-paris/"),
        )
    }

    @Test
    fun websiteDisplayText_stripsPlainHttp() {
        Assert.assertEquals(
            "example.com",
            websiteDisplayText("http://example.com/"),
        )
    }

    @Test
    fun websiteDisplayText_keepsUrlWithoutSchemeUntouched() {
        Assert.assertEquals(
            "example.com/path",
            websiteDisplayText("example.com/path"),
        )
    }

    @Test
    fun upcomingEvents_filtersPastAndSortsAscending() {
        val now = ZonedDateTime.parse("2026-09-16T12:00:00Z")
        val past = event(id = 1, startsAt = "2026-09-15T12:00:00Z")
        val later = event(id = 2, startsAt = "2026-09-18T12:00:00Z")
        val sooner = event(id = 3, startsAt = "2026-09-17T12:00:00Z")

        val result = upcomingEvents(listOf(later, past, sooner), now)

        Assert.assertEquals(listOf(sooner, later), result)
    }

    @Test
    fun upcomingEvents_eventStartingExactlyNowIsExcluded() {
        val now = ZonedDateTime.parse("2026-09-16T12:00:00Z")
        val event = event(id = 1, startsAt = "2026-09-16T12:00:00Z")

        Assert.assertTrue(upcomingEvents(listOf(event), now).isEmpty())
    }

    @Test
    fun describeIssue_knownCodesMapToResourceIds() {
        Assert.assertEquals(R.string.issue_outdated, describeIssue("outdated").resId)
        Assert.assertEquals(R.string.issue_outdated_soon, describeIssue("outdated_soon").resId)
        Assert.assertEquals(R.string.not_verified, describeIssue("not_verified").resId)
        Assert.assertEquals(R.string.issue_missing_icon, describeIssue("missing_icon").resId)
    }

    @Test
    fun describeIssue_invalidTagValueCarriesArgument() {
        val description = describeIssue("invalid_tag_value:name")

        Assert.assertEquals(R.string.issue_invalid_tag_value, description.resId)
        Assert.assertEquals("name", description.formatArg)
    }

    @Test
    fun describeIssue_misspelledTagNameCarriesArgument() {
        val description = describeIssue("misspelled_tag_name:opening_hours")

        Assert.assertEquals(R.string.issue_misspelled_tag_name, description.resId)
        Assert.assertEquals("opening_hours", description.formatArg)
    }

    @Test
    fun describeIssue_unknownCodeFallsBack() {
        val description = describeIssue("something_new")

        Assert.assertEquals(R.string.issue_unknown, description.resId)
        Assert.assertNull(description.formatArg)
    }

    private fun event(id: Long, startsAt: String): GetEventsItem {
        return GetEventsItem(
            id = id,
            areaId = null,
            lat = 0.0,
            lon = 0.0,
            name = "Event $id",
            website = "https://example.com/$id".toHttpUrl(),
            startsAt = ZonedDateTime.parse(startsAt),
            endsAt = null,
        )
    }
}
