package org.btcmap.area

import org.btcmap.i18n.Strings
import org.btcmap.ui.areaStrings
import org.junit.Assert
import org.junit.Test

/** The area issue descriptions, now resolved from the shared string catalog. */
class AreaFormattingTest {

    private val strings = areaStrings(Strings.forLocale("en")) { "0 B" }

    @Test
    fun describeIssue_knownCodesMapToText() {
        Assert.assertEquals("Outdated, needs verification", strings.issueDescription("outdated"))
        Assert.assertEquals("Will be outdated soon", strings.issueDescription("outdated_soon"))
        Assert.assertEquals("Not verified", strings.issueDescription("not_verified"))
        Assert.assertEquals("Missing icon", strings.issueDescription("missing_icon"))
    }

    @Test
    fun describeIssue_invalidTagValueCarriesArgument() {
        Assert.assertEquals(
            "Invalid value for name",
            strings.issueDescription("invalid_tag_value:name"),
        )
    }

    @Test
    fun describeIssue_misspelledTagNameCarriesArgument() {
        Assert.assertEquals(
            "Misspelled tag name: opening_hours",
            strings.issueDescription("misspelled_tag_name:opening_hours"),
        )
    }

    @Test
    fun describeIssue_unknownCodeFallsBack() {
        Assert.assertEquals("Unknown issue", strings.issueDescription("something_new"))
    }
}
