package org.btcmap.db.table.user

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

class UserStoreTest {

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    @Test
    fun insert_and_select() {
        val db = createDatabase()
        val user = createUser(id = 1L, name = "Test User")

        db.user.insert(user)
        val result = db.user.select()

        Assert.assertNotNull(result)
        Assert.assertEquals(1L, result!!.id)
        Assert.assertEquals("Test User", result.name)
    }

    @Test
    fun insert_and_select_roundTripsRolesAndSavedItems() {
        val db = createDatabase()
        val user = createUser(
            id = 1L,
            name = "Test User",
            roles = listOf("user", "admin"),
            savedPlaces = listOf(SavedItem(id = 10L, name = "Bitcoin Cafe")),
            savedAreas = listOf(SavedItem(id = 20L, name = "Grand Paris")),
        )

        db.user.insert(user)
        val result = db.user.select()!!

        Assert.assertEquals(listOf("user", "admin"), result.roles)
        Assert.assertEquals(listOf(SavedItem(10L, "Bitcoin Cafe")), result.savedPlaces)
        Assert.assertEquals(listOf(SavedItem(20L, "Grand Paris")), result.savedAreas)
    }

    @Test
    fun select_returnsNullWhenAbsent() {
        val db = createDatabase()

        val result = db.user.select()

        Assert.assertNull(result)
    }

    @Test
    fun insert_replacesExistingUser() {
        val db = createDatabase()
        val user1 = createUser(id = 1L, name = "Original Name")
        val user2 = createUser(id = 1L, name = "Updated Name")

        db.user.insert(user1)
        db.user.insert(user2)

        val result = db.user.select()

        Assert.assertEquals("Updated Name", result!!.name)
    }

    @Test
    fun delete_removesUser() {
        val db = createDatabase()
        val user = createUser(id = 1L, name = "Test User")

        db.user.insert(user)
        Assert.assertNotNull(db.user.select())

        db.user.delete()

        Assert.assertNull(db.user.select())
    }

    @Test
    fun delete_doesNothingWhenAbsent() {
        val db = createDatabase()

        db.user.delete()

        Assert.assertNull(db.user.select())
    }

    private fun createUser(
        id: Long,
        name: String,
        roles: List<String> = emptyList(),
        savedPlaces: List<SavedItem> = emptyList(),
        savedAreas: List<SavedItem> = emptyList(),
    ): User {
        return User(
            id = id,
            name = name,
            roles = roles,
            savedPlaces = savedPlaces,
            savedAreas = savedAreas,
        )
    }
}
