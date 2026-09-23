package org.btcmap.db

import org.junit.Assert
import org.junit.Test

class StringExtTest {

    @Test
    fun escapeLikePattern_escapesWildcardsAndTheEscapeCharacter() {
        Assert.assertEquals("50\\%", "50%".escapeLikePattern())
        Assert.assertEquals("a\\_b", "a_b".escapeLikePattern())
        Assert.assertEquals("c\\\\d", "c\\d".escapeLikePattern())
        Assert.assertEquals("plain", "plain".escapeLikePattern())
        Assert.assertEquals("", "".escapeLikePattern())
    }
}
