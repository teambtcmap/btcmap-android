package org.btcmap.db.table.user

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.JsonArray
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

class UserQueriesTest {

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
    fun select_returnsNullWhenEmpty() {
        val db = createDatabase()

        val result = db.user.select()

        Assert.assertNull(result)
    }

    @Test
    fun insert_replaceUpdatesExistingUser() {
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
    fun delete_doesNothingWhenEmpty() {
        val db = createDatabase()

        db.user.delete()

        Assert.assertNull(db.user.select())
    }

    private fun createUser(
        id: Long,
        name: String,
        roles: JsonArray = JsonArray(),
        savedPlaces: JsonArray = JsonArray(),
        savedAreas: JsonArray = JsonArray(),
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