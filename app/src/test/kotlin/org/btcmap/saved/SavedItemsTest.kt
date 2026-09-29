package org.btcmap.saved

import org.btcmap.db.table.user.SavedItem
import org.junit.Assert
import org.junit.Test

class SavedItemsTest {

    @Test
    fun withIds_keepsCachedNamesAndServerOrder() {
        val cached = listOf(SavedItem(id = 1, name = "One"), SavedItem(id = 2, name = "Two"))

        val merged = cached.withIds(listOf(2L, 1L))

        Assert.assertEquals(
            listOf(SavedItem(id = 2, name = "Two"), SavedItem(id = 1, name = "One")),
            merged,
        )
    }

    @Test
    fun withIds_namesAnAddedIdFromTheMap() {
        val cached = listOf(SavedItem(id = 1, name = "One"))

        val merged = cached.withIds(listOf(1L, 7L), addedNames = mapOf(7L to "Seven"))

        Assert.assertEquals(
            listOf(SavedItem(id = 1, name = "One"), SavedItem(id = 7, name = "Seven")),
            merged,
        )
    }

    @Test
    fun withIds_returnsNullForAnIdNeitherCachedNorNamed() {
        val cached = listOf(SavedItem(id = 1, name = "One"))

        val merged = cached.withIds(listOf(1L, 99L), addedNames = mapOf(7L to "Seven"))

        Assert.assertNull(merged)
    }

    @Test
    fun withIds_returnsNullWhenTheAddedNameIsBlank() {
        val cached = listOf(SavedItem(id = 1, name = "One"))

        val merged = cached.withIds(listOf(1L, 7L), addedNames = mapOf(7L to "  "))

        Assert.assertNull(merged)
    }

    @Test
    fun withIds_ignoresAnAddedNameForAnIdAlreadyCached() {
        val cached = listOf(SavedItem(id = 1, name = "Cached"))

        val merged = cached.withIds(listOf(1L), addedNames = mapOf(1L to "Server"))

        Assert.assertEquals(listOf(SavedItem(id = 1, name = "Cached")), merged)
    }

    @Test
    fun withIds_returnsEmptyListWhenNothingIsSaved() {
        val cached = listOf(SavedItem(id = 1, name = "One"))

        val merged = cached.withIds(emptyList())

        Assert.assertEquals(emptyList<SavedItem>(), merged)
    }
}
