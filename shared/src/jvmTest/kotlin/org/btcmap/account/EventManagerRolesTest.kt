package org.btcmap.account

import org.btcmap.db.table.user.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventManagerRolesTest {

    private fun user(vararg roles: String) = User(
        id = 1L,
        name = "tester",
        roles = roles.toList(),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )

    @Test
    fun eventManagerAdminAndRootCanManage() {
        assertTrue(user("event_manager").canManageEvents())
        assertTrue(user("admin").canManageEvents())
        assertTrue(user("root").canManageEvents())
        assertTrue(user("user", "event_manager").canManageEvents())
    }

    @Test
    fun regularUsersCannotManage() {
        assertFalse(user("user").canManageEvents())
        assertFalse(user().canManageEvents())
    }
}
