package org.btcmap.db.table.user

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

class UserStoreTest {

    private fun createDatabase(): Database = runBlocking {
        org.btcmap.db.testDatabase().apply { connect() }
    }

    @Test
    fun insert_and_select() = runBlocking<Unit> {
        val db = createDatabase()
        val user = createUser(id = 1L, name = "Test User")

        db.user.insert(user)
        val result = db.user.select()

        Assert.assertNotNull(result)
        Assert.assertEquals(1L, result!!.id)
        Assert.assertEquals("Test User", result.name)
    }

    @Test
    fun insert_and_select_roundTripsRolesAndSavedItems() = runBlocking<Unit> {
        val db = createDatabase()
        val user = createUser(
            id = 1L,
            name = "Test User",
            roles = listOf("user", "admin"),
            savedPlaces = listOf(SavedItem(id = 10L, name = "Bitcoin Cafe")),
            savedAreas = listOf(SavedItem(id = 20L, name = "Grand Paris")),
            geofence = listOf(20L),
        )

        db.user.insert(user)
        val result = db.user.select()!!

        Assert.assertEquals(listOf("user", "admin"), result.roles)
        Assert.assertEquals(listOf(SavedItem(10L, "Bitcoin Cafe")), result.savedPlaces)
        Assert.assertEquals(listOf(SavedItem(20L, "Grand Paris")), result.savedAreas)
        Assert.assertEquals(listOf(20L), result.geofence)
    }

    @Test
    fun select_returnsNullWhenAbsent() = runBlocking<Unit> {
        val db = createDatabase()

        val result = db.user.select()

        Assert.assertNull(result)
    }

    @Test
    fun select_returnsNullForCorruptJson() = runBlocking<Unit> {
        val db = createDatabase()
        // A value written by an older or broken build must read as "no user"
        // instead of throwing out of a read.
        db.preference.upsert(UserStore.KEY, "{not json")

        Assert.assertNull(db.user.select())
    }

    @Test
    fun insert_replacesExistingUser() = runBlocking<Unit> {
        val db = createDatabase()
        val user1 = createUser(id = 1L, name = "Original Name")
        val user2 = createUser(id = 1L, name = "Updated Name")

        db.user.insert(user1)
        db.user.insert(user2)

        val result = db.user.select()

        Assert.assertEquals("Updated Name", result!!.name)
    }

    @Test
    fun delete_removesUser() = runBlocking<Unit> {
        val db = createDatabase()
        val user = createUser(id = 1L, name = "Test User")

        db.user.insert(user)
        Assert.assertNotNull(db.user.select())

        db.user.delete()

        Assert.assertNull(db.user.select())
    }

    @Test
    fun delete_doesNothingWhenAbsent() = runBlocking<Unit> {
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
        geofence: List<Long> = emptyList(),
    ): User {
        return User(
            id = id,
            name = name,
            roles = roles,
            savedPlaces = savedPlaces,
            savedAreas = savedAreas,
            geofence = geofence,
        )
    }
}
