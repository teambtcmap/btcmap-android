package org.btcmap.api

import org.btcmap.db.table.user.SavedItem as DbSavedItem
import org.junit.Assert
import org.junit.Test

class UserExtTest {
    @Test
    fun toDbUser_copiesIdentityRolesAndSavedItems() {
        val user = User(
            id = 124,
            name = "Satoshi",
            roles = listOf("admin", "user"),
            savedPlaces = listOf(SavedItem(id = 1, name = "Bitcoin Cafe")),
            savedAreas = listOf(SavedItem(id = 2, name = "Downtown")),
        )

        val dbUser = user.toDbUser()

        Assert.assertEquals(124L, dbUser.id)
        Assert.assertEquals("Satoshi", dbUser.name)
        Assert.assertEquals(listOf("admin", "user"), dbUser.roles)
        Assert.assertEquals(
            listOf(DbSavedItem(id = 1, name = "Bitcoin Cafe")),
            dbUser.savedPlaces,
        )
        Assert.assertEquals(
            listOf(DbSavedItem(id = 2, name = "Downtown")),
            dbUser.savedAreas,
        )
    }
}
