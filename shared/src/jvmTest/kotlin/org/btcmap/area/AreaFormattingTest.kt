package org.btcmap.area

import org.junit.Assert
import org.junit.Test

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
}
