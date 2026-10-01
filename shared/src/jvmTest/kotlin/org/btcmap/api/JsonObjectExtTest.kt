package org.btcmap.api

import com.google.gson.JsonParser
import org.junit.Assert
import org.junit.Test

class JsonObjectExtTest {
    private fun obj(json: String) = JsonParser.parseString(json).asJsonObject

    @Test
    fun string_returnsValue() {
        Assert.assertEquals("hello", obj("""{"v":"hello"}""").string("v"))
    }

    @Test
    fun string_missingFieldThrowsParseException() {
        val e = assertParseException { obj("{}").string("v") }

        Assert.assertEquals("Missing required JSON field 'v'", e.message)
    }

    @Test
    fun string_explicitNullThrowsParseException() {
        assertParseException { obj("""{"v":null}""").string("v") }
    }

    @Test
    fun string_objectValueThrowsParseException() {
        val e = assertParseException { obj("""{"v":{"nested":1}}""").string("v") }

        Assert.assertEquals("Field 'v' has unexpected type object", e.message)
        Assert.assertNotNull(e.cause)
    }

    @Test
    fun string_arrayValueThrowsParseException() {
        val e = assertParseException { obj("""{"v":[1,2]}""").string("v") }

        Assert.assertEquals("Field 'v' has unexpected type array", e.message)
    }

    @Test
    fun long_nonNumericValueThrowsParseException() {
        val e = assertParseException { obj("""{"v":"abc"}""").long("v") }

        Assert.assertEquals("Field 'v' has unexpected type string", e.message)
    }

    @Test
    fun obj_returnsObject() {
        Assert.assertEquals(1, obj("""{"v":{"n":1}}""").obj("v").int("n"))
    }

    @Test
    fun obj_primitiveValueThrowsParseException() {
        val e = assertParseException { obj("""{"v":42}""").obj("v") }

        Assert.assertEquals("Field 'v' has unexpected type number", e.message)
    }

    @Test
    fun optional_missingAndNullReturnNull() {
        Assert.assertNull(obj("{}").stringOrNull("v"))
        Assert.assertNull(obj("""{"v":null}""").stringOrNull("v"))
    }

    @Test
    fun optional_wrongTypeThrowsParseException() {
        val e = assertParseException { obj("""{"v":{"n":1}}""").stringOrNull("v") }

        Assert.assertEquals("Field 'v' has unexpected type object", e.message)
    }

    @Test
    fun nonBlankStringOrNull_returnsNullForBlank() {
        Assert.assertNull(obj("""{"v":"  "}""").nonBlankStringOrNull("v"))
    }

    @Test
    fun doubleArrayOrNull_parsesNumbers() {
        Assert.assertEquals(listOf(1.0, 2.5), obj("""{"v":[1,2.5]}""").doubleArrayOrNull("v"))
    }

    @Test
    fun doubleArrayOrNull_missingReturnsNull() {
        Assert.assertNull(obj("{}").doubleArrayOrNull("v"))
    }

    @Test
    fun doubleArrayOrNull_wrongElementTypeThrowsParseException() {
        val e = assertParseException { obj("""{"v":[1,"x"]}""").doubleArrayOrNull("v") }

        Assert.assertEquals("Field 'v' has unexpected type array", e.message)
    }

    private fun assertParseException(block: () -> Unit): ApiParseException {
        try {
            block()
        } catch (e: ApiParseException) {
            return e
        }

        Assert.fail("Expected ApiParseException")
        throw AssertionError("unreachable")
    }
}
