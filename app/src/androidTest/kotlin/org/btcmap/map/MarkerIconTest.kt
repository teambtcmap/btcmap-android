package org.btcmap.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkerIconTest {
    @Test
    fun recognizesIconsPresentInBundledFont() {
        assertTrue(isRenderableIcon("menu_book"))
        assertTrue(isRenderableIcon("storefront"))
        assertTrue(isRenderableIcon("wc"))
    }

    @Test
    fun rejectsIconsMissingFromBundledFont() {
        assertFalse(isRenderableIcon("foo_bar"))
        assertFalse(isRenderableIcon("zz"))
    }
}
