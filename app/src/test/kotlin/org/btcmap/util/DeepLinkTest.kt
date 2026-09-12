package org.btcmap.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert
import org.junit.Test

class DeepLinkTest {
    @Test
    fun merchantUrl_returnsPlaceId() {
        Assert.assertEquals(
            6556L,
            "https://btcmap.org/merchant/6556".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun merchantUrlWithTrailingSlash_returnsPlaceId() {
        Assert.assertEquals(
            6556L,
            "https://btcmap.org/merchant/6556/".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun httpScheme_returnsPlaceId() {
        Assert.assertEquals(
            6556L,
            "http://btcmap.org/merchant/6556".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun nonMerchantPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/community/prague".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun nonNumericPlaceId_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/abc".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun otherHost_returnsNull() {
        Assert.assertNull(
            "https://example.com/merchant/6556".toHttpUrl().deepLinkPlaceId(),
        )
    }

    @Test
    fun nestedPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/6556/extra".toHttpUrl().deepLinkPlaceId(),
        )
    }
}
