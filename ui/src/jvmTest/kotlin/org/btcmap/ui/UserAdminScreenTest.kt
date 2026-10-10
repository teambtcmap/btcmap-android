package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The user admin screen: the field projection and the read-only field list it
 * renders.
 */
class UserAdminScreenTest {

    private fun user(
        geofence: List<Long> = listOf(3L, 7L),
        npub: String? = null,
    ) = ManageUserUi(
        id = 124,
        name = "natinfosec",
        roles = listOf("user", "area_manager"),
        createdAt = "2024-01-01T00:00:00Z",
        geofence = geofence,
        npub = npub,
    )

    @Test
    fun userAdminFields_listTheRecord() {
        val fields = userAdminFields(user(npub = "npub1...")).associate { it.label to it.value }

        assertEquals("124", fields["id"])
        assertEquals("natinfosec", fields["name"])
        assertEquals("user, area_manager", fields["roles"])
        assertEquals("2024-01-01T00:00:00Z", fields["created_at"])
        assertEquals("3, 7", fields["geofence"])
        assertEquals("npub1...", fields["npub"])
    }

    @Test
    fun userAdminFields_showGeofenceAreaNamesWhenResolved() {
        val fields = userAdminFields(user(), geofenceLabels = listOf("Grand Paris", "Berlin"))
            .associate { it.label to it.value }

        assertEquals("Grand Paris, Berlin", fields["geofence"])
    }

    @Test
    fun userAdminFields_placeholdersForAbsentValues() {
        val fields = userAdminFields(user(geofence = emptyList())).associate { it.label to it.value }

        assertEquals("—", fields["geofence"])
        assertEquals("—", fields["npub"])
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheFields() {
        runComposeUiTest {
            setContent { AppTheme { UserAdminScreen(user(npub = "npub1...")) } }
            onNodeWithText("natinfosec").assertExists()
            onNodeWithText("user, area_manager").assertExists()
            onNodeWithText("3, 7").assertExists()
            onNodeWithText("npub1...").assertExists()
        }
    }

    @Test
    fun rootEditsEveryRoleExceptRoot() {
        val edit = userRolesEdit(CallerRoles(1, listOf("root")), user())

        assertEquals(setOf("user", "event_manager", "area_manager", "admin"), edit.enabled)
        assertTrue("root" !in edit.enabled)
    }

    @Test
    fun rootCannotEditItsOwnRoles() {
        assertFalse(userRolesEdit(CallerRoles(124, listOf("root")), user()).canEdit)
    }

    @Test
    fun rootCannotEditAnotherRoot() {
        val target = user().copy(roles = listOf("user", "root"))

        assertFalse(userRolesEdit(CallerRoles(1, listOf("root")), target).canEdit)
    }

    @Test
    fun adminOnlyEditsTheManagerRolesOfANormalUser() {
        val edit = userRolesEdit(CallerRoles(1, listOf("admin")), user())

        assertEquals(setOf("event_manager", "area_manager"), edit.enabled)
    }

    @Test
    fun adminCannotEditAnotherAdminOrItself() {
        val otherAdmin = user().copy(roles = listOf("user", "admin"))

        assertFalse(userRolesEdit(CallerRoles(1, listOf("admin")), otherAdmin).canEdit)
        assertFalse(userRolesEdit(CallerRoles(124, listOf("admin")), user()).canEdit)
    }

    @Test
    fun toRolesPreservesRolesOutsideTheDialog() {
        val target = user().copy(roles = listOf("user", "places_source"))
        val edit = userRolesEdit(CallerRoles(1, listOf("root")), target)

        assertEquals(setOf("places_source"), edit.hidden)
        assertEquals(
            listOf("user", "area_manager", "places_source"),
            edit.toRoles(setOf("user", "area_manager")),
        )
    }

    @Test
    fun rootCanEditAnyNonRootGeofenceAndItsOwn() {
        assertTrue(canEditGeofence(CallerRoles(1, listOf("root")), user()))
        assertTrue(
            canEditGeofence(
                CallerRoles(124, listOf("root")),
                user().copy(roles = listOf("root")),
            ),
        )
    }

    @Test
    fun rootCannotEditAnotherRootGeofence() {
        val otherRoot = user().copy(id = 2, roles = listOf("root"))

        assertFalse(canEditGeofence(CallerRoles(1, listOf("root")), otherRoot))
    }

    @Test
    fun adminCanEditItsOwnAndANormalUsersGeofence() {
        assertTrue(
            canEditGeofence(
                CallerRoles(124, listOf("admin")),
                user().copy(roles = listOf("user", "admin")),
            ),
        )
        assertTrue(canEditGeofence(CallerRoles(1, listOf("admin")), user()))
    }

    @Test
    fun adminCannotEditAnotherAdminOrARootGeofence() {
        val otherAdmin = user().copy(id = 2, roles = listOf("user", "admin"))
        val root = user().copy(id = 3, roles = listOf("root"))

        assertFalse(canEditGeofence(CallerRoles(1, listOf("admin")), otherAdmin))
        assertFalse(canEditGeofence(CallerRoles(1, listOf("admin")), root))
    }
}
