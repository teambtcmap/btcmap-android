package org.btcmap.util

import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Test

class DeepLinkTest {
    @Test
    fun merchantUrl_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "https://btcmap.org/merchant/6556".toUrl().deepLink(),
        )
    }

    @Test
    fun merchantUrlWithTrailingSlash_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "https://btcmap.org/merchant/6556/".toUrl().deepLink(),
        )
    }

    @Test
    fun httpScheme_returnsPlaceDeepLink() {
        Assert.assertEquals(
            DeepLink.Place(6556L),
            "http://btcmap.org/merchant/6556".toUrl().deepLink(),
        )
    }

    @Test
    fun eventUrl_returnsEventDeepLink() {
        Assert.assertEquals(
            DeepLink.Event(42L),
            "https://btcmap.org/event/42".toUrl().deepLink(),
        )
    }

    @Test
    fun eventUrlWithTrailingSlash_returnsEventDeepLink() {
        Assert.assertEquals(
            DeepLink.Event(42L),
            "https://btcmap.org/event/42/".toUrl().deepLink(),
        )
    }

    @Test
    fun nonDeepLinkPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/community/prague".toUrl().deepLink(),
        )
    }

    @Test
    fun nonNumericMerchantId_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/abc".toUrl().deepLink(),
        )
    }

    @Test
    fun nonNumericEventId_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/event/abc".toUrl().deepLink(),
        )
    }

    @Test
    fun otherHost_returnsNull() {
        Assert.assertNull(
            "https://example.com/merchant/6556".toUrl().deepLink(),
        )
    }

    @Test
    fun nestedPath_returnsNull() {
        Assert.assertNull(
            "https://btcmap.org/merchant/6556/extra".toUrl().deepLink(),
        )
    }
}
