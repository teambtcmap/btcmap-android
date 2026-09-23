package org.btcmap.api

import org.junit.Assert
import org.junit.Test

class ApiErrorTest {
    @Test
    fun apiException_isRetryableOnlyForServerErrors() {
        Assert.assertTrue(ApiException(code = 500, message = "server error").retryable)
        Assert.assertTrue(ApiException(code = 503, message = "server error").retryable)
        Assert.assertFalse(ApiException(code = 400, message = "bad request").retryable)
        Assert.assertFalse(ApiException(code = 401, message = "unauthorized").retryable)
        Assert.assertFalse(ApiException(code = 404, message = "not found").retryable)
    }

    @Test
    fun apiTransportException_isRetryable() {
        Assert.assertTrue(ApiTransportException("offline").retryable)
    }

    @Test
    fun apiParseException_isNotRetryable() {
        Assert.assertFalse(ApiParseException("unexpected body").retryable)
    }

    @Test
    fun everyApiFailureCanBeCaughtAsApiError() {
        val errors = listOf<ApiError>(
            ApiException(code = 500, message = "x"),
            ApiParseException("x"),
            ApiTransportException("x"),
        )

        errors.forEach { error ->
            val caught = try {
                throw error
            } catch (e: ApiError) {
                e
            }

            Assert.assertSame(error, caught)
        }
    }
}
