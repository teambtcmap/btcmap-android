package org.btcmap.i18n

import kotlinx.serialization.json.JsonObject
import org.btcmap.json.parseJsonObject
import org.junit.Assert
import org.junit.Test

class JsonObjectTest {

    private fun json(text: String): JsonObject = parseJsonObject(text)

    @Test
    fun stringAt_returnsTheString() {
        Assert.assertEquals("Paris", json("""{"en":"Paris"}""").stringAt("en"))
    }

    @Test
    fun stringAt_returnsNullWhenMissing() {
        Assert.assertNull(json("""{"en":"Paris"}""").stringAt("ru"))
    }

    @Test
    fun stringAt_treatsBlankAsAbsent() {
        Assert.assertNull(json("""{"de":"","fr":"   "}""").stringAt("de"))
        Assert.assertNull(json("""{"de":"","fr":"   "}""").stringAt("fr"))
    }

    @Test
    fun stringAt_ignoresNonStringValues() {
        val json = json("""{"n":null,"bool":true,"num":123,"arr":[1],"obj":{"a":1}}""")

        Assert.assertNull(json.stringAt("n"))
        Assert.assertNull(json.stringAt("bool"))
        Assert.assertNull(json.stringAt("num"))
        Assert.assertNull(json.stringAt("arr"))
        Assert.assertNull(json.stringAt("obj"))
    }

    @Test
    fun translated_prefersTheLocale() {
        val json = json("""{"en":"Paris","ru":"Париж"}""")

        Assert.assertEquals("Париж", json.translated("ru"))
    }

    @Test
    fun translated_fallsBackToEnglish() {
        val json = json("""{"en":"Paris","ru":"Париж"}""")

        Assert.assertEquals("Paris", json.translated("de"))
    }

    @Test
    fun translated_skipsABlankOrNonStringLocaleValue() {
        Assert.assertEquals("Paris", json("""{"en":"Paris","de":""}""").translated("de"))
        Assert.assertEquals("Paris", json("""{"en":"Paris","fr":123}""").translated("fr"))
    }

    @Test
    fun translated_isNullWhenNeitherIsPresent() {
        Assert.assertNull(json("""{"ru":"Париж"}""").translated("de"))
    }

    @Test
    fun translated_isNullForNullReceiver() {
        val missing: JsonObject? = null

        Assert.assertNull(missing.translated("ru"))
    }

    @Test
    fun localizedValues_returnsEveryNonBlankString() {
        val json = json("""{"en":"Paris","ru":"Париж","de":"","fr":123,"it":null,"es":{"a":1}}""")
        val values = json.localizedValues()

        Assert.assertEquals(setOf("Paris", "Париж"), values.toSet())
        Assert.assertEquals(2, values.size)
    }

    @Test
    fun localizedValues_isEmptyForNullReceiver() {
        val missing: JsonObject? = null

        Assert.assertTrue(missing.localizedValues().isEmpty())
    }
}
