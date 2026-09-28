package org.btcmap.map

import org.junit.Assert
import org.junit.Test

class FeatureStoreTest {

    private data class Item(val id: Long)

    private fun store(maxItems: Int = 10) = FeatureStore<Item>({ it.id }, maxItems = maxItems)

    @Test
    fun merge_deduplicatesById() {
        val store = store()

        Assert.assertEquals(setOf(Item(1), Item(2)), store.merge(listOf(Item(1), Item(2))))
        // The same ids add nothing, so no new snapshot is emitted.
        Assert.assertNull(store.merge(listOf(Item(2), Item(1))))
        Assert.assertEquals(2, store.size)
    }

    @Test
    fun merge_accumulatesAcrossQueries() {
        val store = store()
        store.merge(listOf(Item(1)))

        // A later query keeps what was already loaded and adds the new feature.
        Assert.assertEquals(setOf(Item(1), Item(2)), store.merge(listOf(Item(2))))
    }

    @Test
    fun merge_resetsToCurrentQueryWhenCapWouldBeExceeded() {
        val store = store(maxItems = 3)
        store.merge(listOf(Item(1), Item(2), Item(3)))
        Assert.assertEquals(3, store.size)

        // A fourth feature would push past the cap, so the history is dropped
        // and only the features of the current query are kept.
        Assert.assertEquals(setOf(Item(4)), store.merge(listOf(Item(4))))
        Assert.assertEquals(1, store.size)
    }

    @Test
    fun merge_canExceedCapWhenASingleQueryDoes() {
        val store = store(maxItems = 2)

        // A viewport that legitimately holds more than the cap is kept whole:
        // dropping some of the current query would hide visible markers.
        Assert.assertEquals(setOf(Item(1), Item(2), Item(3)), store.merge(listOf(Item(1), Item(2), Item(3))))
        Assert.assertEquals(3, store.size)
    }

    @Test
    fun clear_dropsEverything() {
        val store = store()
        store.merge(listOf(Item(1)))

        store.clear()

        Assert.assertEquals(0, store.size)
        Assert.assertEquals(setOf(Item(2)), store.merge(listOf(Item(2))))
    }
}
