package org.btcmap.util

import org.junit.Assert
import org.junit.Test

class StringExtTest {

    @Test
    fun abbreviateHome_replacesTheHomePrefix() {
        Assert.assertEquals(
            "~/cache/coil3_disk_cache",
            "/data/user/0/org.btcmap.debug/cache/coil3_disk_cache"
                .abbreviateHome("/data/user/0/org.btcmap.debug"),
        )
    }

    @Test
    fun abbreviateHome_abbreviatesTheHomeItself() {
        val home = "/data/user/0/org.btcmap.debug"
        Assert.assertEquals("~", home.abbreviateHome(home))
    }

    @Test
    fun abbreviateHome_keepsUnrelatedPaths() {
        Assert.assertEquals(
            "/sdcard/Download",
            "/sdcard/Download".abbreviateHome("/data/user/0/org.btcmap.debug"),
        )
    }

    @Test
    fun abbreviateHome_doesNotMatchAPartialSegment() {
        Assert.assertEquals(
            "/data/user/0/org.btcmap.debugger/cache",
            "/data/user/0/org.btcmap.debugger/cache".abbreviateHome("/data/user/0/org.btcmap.debug"),
        )
    }

    @Test
    fun abbreviateHome_ignoresATrailingSlashOnHome() {
        Assert.assertEquals(
            "~/cache",
            "/data/user/0/org.btcmap.debug/cache".abbreviateHome("/data/user/0/org.btcmap.debug/"),
        )
    }

    @Test
    fun abbreviateHome_returnsThePathWhenHomeIsEmpty() {
        Assert.assertEquals("/cache", "/cache".abbreviateHome(""))
    }
}
