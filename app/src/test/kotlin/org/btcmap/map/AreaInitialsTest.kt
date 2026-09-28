package org.btcmap.map

import org.junit.Assert
import org.junit.Test

class AreaInitialsTest {

    @Test
    fun initials_country_usesTheTwoLetterAlias() {
        Assert.assertEquals("CW", areaInitials(country("Curaçao", "cw")))
    }

    @Test
    fun initials_country_ignoresTheName() {
        // The alias is the ISO code the country is known by, even when the
        // localized name starts with different letters.
        Assert.assertEquals("DE", areaInitials(country("Deutschland", "de")))
    }

    @Test
    fun initials_community_usesTheFirstLetterOfEachWord() {
        Assert.assertEquals("BC", areaInitials(community("Bitcoin Curacao")))
    }

    @Test
    fun initials_community_takesOnlyTheFirstTwoWords() {
        Assert.assertEquals("AB", areaInitials(community("Albany Bitcoin Group")))
    }

    @Test
    fun initials_community_singleWord_usesTheFirstTwoLetters() {
        Assert.assertEquals("PH", areaInitials(community("Phuket")))
    }

    @Test
    fun initials_community_ignoresLeadingWhitespace() {
        Assert.assertEquals("BI", areaInitials(community("  Bitcoin ")))
    }

    @Test
    fun initials_community_skipsLeadingNonLetters() {
        Assert.assertEquals("AB", areaInitials(community("🍊 Alicante Bitcoin")))
    }

    @Test
    fun initials_community_returnsEmptyForABlankName() {
        Assert.assertEquals("", areaInitials(community("   ")))
    }

    private fun country(name: String, urlAlias: String) = area(name, "country", urlAlias)

    private fun community(name: String) = area(name, "community", "alias")

    private fun area(name: String, type: String, urlAlias: String) = MapArea(
        id = 1L,
        name = name,
        type = type,
        urlAlias = urlAlias,
        upcomingEventsCount = 0,
        headerImageUrl = null,
    )
}
