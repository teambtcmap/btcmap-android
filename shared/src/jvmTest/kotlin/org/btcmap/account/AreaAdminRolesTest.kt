package org.btcmap.account

import org.btcmap.db.table.user.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaAdminRolesTest {

    private fun user(vararg roles: String) = User(
        id = 1L,
        name = "tester",
        roles = roles.toList(),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )

    @Test
    fun areaManagerAndAdminAndRootCanManage() {
        assertTrue(user("area_manager").canManageAreas())
        assertTrue(user("admin").canManageAreas())
        assertTrue(user("root").canManageAreas())
        assertTrue(user("user", "area_manager").canManageAreas())
    }

    @Test
    fun regularUsersCannotManage() {
        assertFalse(user("user").canManageAreas())
        assertFalse(user("event_manager").canManageAreas())
        assertFalse(user().canManageAreas())
    }
}
