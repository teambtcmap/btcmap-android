package org.btcmap.ui.map

import org.btcmap.map.MapArea
import kotlin.test.Test
import kotlin.test.assertEquals

class AreaInitialsTest {

    @Test
    fun initials_country_usesTheTwoLetterAlias() {
        assertEquals("CW", areaInitials(country("Curaçao", "cw")))
    }

    @Test
    fun initials_country_ignoresTheName() {
        // The alias is the ISO code the country is known by, even when the
        // localized name starts with different letters.
        assertEquals("DE", areaInitials(country("Deutschland", "de")))
    }

    @Test
    fun initials_community_usesTheFirstLetterOfEachWord() {
        assertEquals("BC", areaInitials(community("Bitcoin Curacao")))
    }

    @Test
    fun initials_community_takesOnlyTheFirstTwoWords() {
        assertEquals("AB", areaInitials(community("Albany Bitcoin Group")))
    }

    @Test
    fun initials_community_singleWord_usesTheFirstTwoLetters() {
        assertEquals("PH", areaInitials(community("Phuket")))
    }

    @Test
    fun initials_community_ignoresLeadingWhitespace() {
        assertEquals("BI", areaInitials(community("  Bitcoin ")))
    }

    @Test
    fun initials_community_skipsLeadingNonLetters() {
        assertEquals("AB", areaInitials(community("🍊 Alicante Bitcoin")))
    }

    @Test
    fun initials_community_returnsEmptyForABlankName() {
        assertEquals("", areaInitials(community("   ")))
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
