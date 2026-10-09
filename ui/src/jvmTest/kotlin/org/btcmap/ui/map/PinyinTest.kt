package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The pinyin ruby a Han-script name is drawn with, over jpinyin: one reading per
 * character, and ruby only for names that are mostly Han.
 */
class PinyinTest {

    @Test
    fun givesAReadingPerHanCharacter() {
        assertEquals(listOf("bei", "jing", "fan", "dian"), rubyPinyin("北京饭店"))
    }

    @Test
    fun readsAPolyphonicCharacterFromItsContext() {
        // 行 is xíng alone but háng in 银行.
        assertEquals(listOf("yin", "hang"), rubyPinyin("银行"))
    }

    @Test
    fun keepsOneSyllablePerCharacterWhenADictionaryWordWouldMerge() {
        // jpinyin would otherwise read 智选 as the single token "zhixuan".
        assertEquals(
            listOf("dong", "zhi", "men", "zhi", "xuan", "jia", "ri", "jiu", "dian"),
            rubyPinyin("东直门智选假日酒店"),
        )
    }

    @Test
    fun keepsPlaceholdersForNonHanCharacters() {
        assertEquals(listOf("bei", null, "jing"), rubyPinyin("北 京"))
    }

    @Test
    fun doesNotRubyALatinName() {
        assertNull(rubyPinyin("Harry's Bar"))
    }

    @Test
    fun doesNotRubyAMostlyLatinName() {
        assertNull(rubyPinyin("Beijing 北京"))
    }
}
