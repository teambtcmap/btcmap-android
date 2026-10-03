package org.btcmap.area

import org.btcmap.R
import org.junit.Assert
import org.junit.Test

/** Only the resource-bound half of the area formatting: the rest lives in `:shared`. */
class AreaFormattingTest {

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
}
