package org.btcmap.search

import org.junit.Assert
import org.junit.Test

class NameMatchTest {

    @Test
    fun ranksExactMatchHighest() {
        Assert.assertEquals(0, nameMatchRank(listOf("Paris"), "paris"))
    }

    @Test
    fun ranksPrefixAboveSubstring() {
        Assert.assertEquals(1, nameMatchRank(listOf("Paris Cafe"), "paris"))
        Assert.assertEquals(2, nameMatchRank(listOf("Cafe de Paris"), "paris"))
    }

    @Test
    fun keepsTheBestRankAcrossEveryName() {
        // The base name is only a substring, but a translation is an exact match.
        val names = listOf("Cafe de Paris", "Париж")

        Assert.assertEquals(0, nameMatchRank(names, "Париж"))
        Assert.assertEquals(2, nameMatchRank(names, "paris"))
    }

    @Test
    fun matchesCaseInsensitively() {
        Assert.assertEquals(0, nameMatchRank(listOf("Париж"), "париж"))
        Assert.assertEquals(1, nameMatchRank(listOf("Paris Cafe"), "PARIS"))
    }

    @Test
    fun returnsNullWhenNoNameContainsTheQuery() {
        Assert.assertNull(nameMatchRank(listOf("Paris", "Париж"), "berlin"))
    }

    @Test
    fun returnsNullForNoNames() {
        Assert.assertNull(nameMatchRank(emptyList(), "paris"))
    }
}
