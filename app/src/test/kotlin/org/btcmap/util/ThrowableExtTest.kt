package org.btcmap.util

import org.btcmap.api.ApiException
import org.junit.Assert
import org.junit.Test

class ThrowableExtTest {

    @Test
    fun returnsServerMessageForClientError() {
        Assert.assertEquals(
            "Invalid credentials",
            ApiException(401, "Invalid credentials").userFacingMessage("fallback"),
        )
    }

    @Test
    fun fallsBackForServerError() {
        Assert.assertEquals(
            "fallback",
            ApiException(500, "database exploded").userFacingMessage("fallback"),
        )
    }

    @Test
    fun fallsBackForBlankMessage() {
        Assert.assertEquals(
            "fallback",
            ApiException(400, "  ").userFacingMessage("fallback"),
        )
    }

    @Test
    fun fallsBackForNonApiException() {
        Assert.assertEquals(
            "fallback",
            IllegalStateException("Failed to reach https://api.btcmap.org").userFacingMessage("fallback"),
        )
    }
}
