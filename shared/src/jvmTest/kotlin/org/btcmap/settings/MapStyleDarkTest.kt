package org.btcmap.settings

import org.junit.Assert
import org.junit.Test

/**
 * The map style's darkness drives the system-bar icon tint (see
 * `PlatformSystemBarIcons`), so a light style in a dark app must report light.
 */
class MapStyleDarkTest {

    @Test
    fun darkStyles_areDarkInEitherSystemTheme() {
        for (style in listOf(MapStyle.Dark, MapStyle.DarkMatter)) {
            Assert.assertTrue(style.isDark(darkSystemTheme = false))
            Assert.assertTrue(style.isDark(darkSystemTheme = true))
        }
    }

    @Test
    fun lightStyles_areLightInEitherSystemTheme() {
        for (style in listOf(MapStyle.Liberty, MapStyle.Positron, MapStyle.Bright)) {
            Assert.assertFalse(style.isDark(darkSystemTheme = false))
            Assert.assertFalse(style.isDark(darkSystemTheme = true))
        }
    }

    @Test
    fun auto_followsTheSystemTheme() {
        Assert.assertFalse(MapStyle.Auto.isDark(darkSystemTheme = false))
        Assert.assertTrue(MapStyle.Auto.isDark(darkSystemTheme = true))
    }
}
