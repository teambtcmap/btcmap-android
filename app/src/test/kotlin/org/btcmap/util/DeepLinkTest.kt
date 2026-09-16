package org.btcmap.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert
import org.junit.Test

class DeepLinkTest {
    @Test
    fun merchantUrl_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "https://btcmap.org/merchant/6556".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun merchantUrlWithTrailingSlash_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "https://btcmap.org/merchant/6556/".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun httpScheme_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "http://btcmap.org/merchant/6556".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun eventUrl_returnsEventDeepLink() {
        Assert.assertEquals(
            DeepLink.Event(42L),
            "https://btcmap.org/event/42".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun eventUrlWithTrailingSlash_returnsEventDeepLink() {
        Assert.assertEquals(
            DeepLink.Event(42L),
            "https://btcmap.org/event/42/".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun nonDeepLinkPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/community/prague".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun nonNumericMerchantId_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/abc".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun nonNumericEventId_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/event/abc".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun otherHost_returnsNull() {
        Assert.assertNull(
            "https://example.com/merchant/6556".toHttpUrl().deepLink(),
        )
    }

    @Test
    fun nestedPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/6556/extra".toHttpUrl().deepLink(),
        )
    }
}
