package org.btcmap.map

import org.junit.Assert
import org.junit.Test

class MarkerImageRegistryTest {

    @Test
    fun merchantMasks_areStoredAndFoundByName() {
        val registry = MarkerImageRegistry()

        Assert.assertFalse(registry.hasMerchantMask("merchant-marker-storefront"))
        registry.addMerchantMask("merchant-marker-storefront", mask())

        Assert.assertTrue(registry.hasMerchantMask("merchant-marker-storefront"))
        Assert.assertNotNull(registry.merchantMask("merchant-marker-storefront"))
        Assert.assertEquals(setOf("merchant-marker-storefront"), registry.merchantMaskNames())
    }

    @Test
    fun exchangeImages_areTracked() {
        val registry = MarkerImageRegistry()

        Assert.assertFalse(registry.hasExchangeImage("marker-icon-storefront"))
        registry.addExchangeImage("marker-icon-storefront")

        Assert.assertTrue(registry.hasExchangeImage("marker-icon-storefront"))
        Assert.assertEquals(setOf("marker-icon-storefront"), registry.exchangeImageNames())
    }

    @Test
    fun pinMask_isStoredAndCleared() {
        val registry = MarkerImageRegistry()

        Assert.assertNull(registry.pinMask())
        registry.addPinMask(mask())
        Assert.assertNotNull(registry.pinMask())

        registry.clear()

        Assert.assertNull(registry.pinMask())
    }

    @Test
    fun clear_dropsEverything() {
        val registry = MarkerImageRegistry()
        registry.addMerchantMask("a", mask())
        registry.addExchangeImage("b")

        registry.clear()

        Assert.assertFalse(registry.hasMerchantMask("a"))
        Assert.assertFalse(registry.hasExchangeImage("b"))
        Assert.assertTrue(registry.merchantMaskNames().isEmpty())
        Assert.assertTrue(registry.exchangeImageNames().isEmpty())
    }

    @Test
    fun registriesAreIndependent() {
        // Two live maps must not invalidate each other's masks: one map's clear
        // cannot touch the other's registry.
        val first = MarkerImageRegistry()
        val second = MarkerImageRegistry()
        first.addMerchantMask("a", mask())

        Assert.assertFalse(second.hasMerchantMask("a"))

        second.clear()

        Assert.assertTrue(first.hasMerchantMask("a"))
    }

    private fun mask() = AlphaMask(width = 1, height = 1, bits = longArrayOf(1L))
}
