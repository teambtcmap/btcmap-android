package org.btcmap.auth

import org.junit.Assert
import org.junit.Test

class AuthTokenLabelTest {

    @Test
    fun includesVersionAndDevice() {
        Assert.assertEquals(
            "BTC Map Android 89 Google Pixel 7",
            authTokenLabel(manufacturer = "Google", model = "Pixel 7", versionCode = 89),
        )
    }

    @Test
    fun trimsEachDeviceValue() {
        Assert.assertEquals(
            "BTC Map Android 89 Google Pixel 7",
            authTokenLabel(manufacturer = "  Google ", model = " Pixel 7", versionCode = 89),
        )
    }

    @Test
    fun omitsNullDeviceValues() {
        // Some devices report no manufacturer or model, which used to crash.
        Assert.assertEquals(
            "BTC Map Android 89",
            authTokenLabel(manufacturer = null, model = null, versionCode = 89),
        )
    }

    @Test
    fun omitsBlankDeviceValues() {
        Assert.assertEquals(
            "BTC Map Android 89",
            authTokenLabel(manufacturer = "  ", model = "", versionCode = 89),
        )
    }

    @Test
    fun keepsTheOneDeviceValueThatIsPresent() {
        Assert.assertEquals(
            "BTC Map Android 89 Pixel 7",
            authTokenLabel(manufacturer = null, model = "Pixel 7", versionCode = 89),
        )
    }
}
