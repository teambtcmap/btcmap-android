package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.i18n.Strings
import org.btcmap.settings.MapStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The user admin page: the edit-roles and edit-geofence actions appear only when
 * the caller may change them, the dialogs lock or filter what the caller cannot
 * touch, and a save goes through the host.
 */
class UserAdminPageTest {

    private fun labels(): AppLabels = appLabels(
        strings = Strings.current(),
        currentStyle = MapStyle.Auto,
        formatNumber = { value, _ -> value.toString() },
        formatBytes = { "$it B" },
        formatFeedDate = { it },
    )

    private fun user(roles: List<String> = listOf("user")) = ManageUserUi(
        id = 124,
        name = "natinfosec",
        roles = roles,
        createdAt = "2024-01-01T00:00:00Z",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun adminTogglesAManagerRoleAndSavesThroughTheHost() {
        var saved: List<String>? = null
        val labels = labels()
        runComposeUiTest {
            setContent {
                AppTheme {
                    UserAdminPage(
                        user = user(),
                        labels = labels,
                        loadCaller = { CallerRoles(1, listOf("admin")) },
                        loadAreaName = { null },
                        loadAreas = { emptyList() },
                        updateRoles = { _, roles ->
                            saved = roles
                            user(roles)
                        },
                        updateGeofence = { _, _ -> user() },
                        onBack = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(userAdminActionTag("roles")).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(userAdminActionTag("roles")).performClick()
            waitForIdle()

            // An admin may only touch the manager roles; the rest are locked.
            onNodeWithTag(userAdminRoleTag("root")).assertIsNotEnabled()
            onNodeWithTag(userAdminRoleTag("user")).assertIsNotEnabled()
            onNodeWithTag(userAdminRoleTag("admin")).assertIsNotEnabled()
            onNodeWithTag(userAdminRoleTag("area_manager")).assertIsEnabled()

            onNodeWithTag(userAdminRoleTag("area_manager")).performClick()
            onNodeWithText(labels.save).performClick()
            waitUntil(timeoutMillis = 5_000) { saved != null }
        }

        assertEquals(listOf("user", "area_manager"), saved)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rootPromotesToAdminButCannotTickRoot() {
        var saved: List<String>? = null
        val labels = labels()
        runComposeUiTest {
            setContent {
                AppTheme {
                    UserAdminPage(
                        user = user(),
                        labels = labels,
                        loadCaller = { CallerRoles(1, listOf("root")) },
                        loadAreaName = { null },
                        loadAreas = { emptyList() },
                        updateRoles = { _, roles ->
                            saved = roles
                            user(roles)
                        },
                        updateGeofence = { _, _ -> user() },
                        onBack = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(userAdminActionTag("roles")).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(userAdminActionTag("roles")).performClick()
            waitForIdle()

            // Root may grant admin, but `root` itself is never assignable.
            onNodeWithTag(userAdminRoleTag("root")).assertIsNotEnabled()
            onNodeWithTag(userAdminRoleTag("admin")).assertIsEnabled()

            onNodeWithTag(userAdminRoleTag("admin")).performClick()
            onNodeWithText(labels.save).performClick()
            waitUntil(timeoutMillis = 5_000) { saved != null }
        }

        assertEquals(listOf("user", "admin"), saved)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editActionsAreHiddenWhenTheCallerCannotEdit() {
        val labels = labels()
        runComposeUiTest {
            setContent {
                AppTheme {
                    UserAdminPage(
                        user = user(roles = listOf("user", "admin")),
                        labels = labels,
                        loadCaller = { CallerRoles(1, listOf("admin")) },
                        loadAreaName = { null },
                        loadAreas = { emptyList() },
                        updateRoles = { _, _ -> user() },
                        updateGeofence = { _, _ -> user() },
                        onBack = {},
                    )
                }
            }
            waitForIdle()

            onAllNodesWithTag(userAdminActionTag("roles")).assertCountEquals(0)
            onAllNodesWithTag(userAdminActionTag("geofence")).assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun adminFiltersTheGeofenceAndSavesThroughTheHost() {
        var saved: List<Long>? = null
        val labels = labels()
        val areas = listOf(
            GeofenceAreaUi(1, "Grand Paris", "community"),
            GeofenceAreaUi(2, "Berlin", "community"),
            GeofenceAreaUi(3, "Portugal", "country"),
        )
        runComposeUiTest {
            setContent {
                AppTheme {
                    UserAdminPage(
                        user = user(),
                        labels = labels,
                        loadCaller = { CallerRoles(1, listOf("admin")) },
                        loadAreaName = { null },
                        loadAreas = { areas },
                        updateRoles = { _, _ -> user() },
                        updateGeofence = { _, ids ->
                            saved = ids
                            user().copy(geofence = ids)
                        },
                        onBack = {},
                    )
                }
            }
            // The roles action appearing means the caller has loaded.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(userAdminActionTag("roles")).fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasScrollAction())
                .performScrollToNode(hasTestTag(userAdminActionTag("geofence")))
            onNodeWithTag(userAdminActionTag("geofence")).performClick()

            // The area list loads on open; filter it, then tick an area.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(userAdminGeofenceAreaTag(1)).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(USER_ADMIN_GEOFENCE_SEARCH_TAG).performTextInput("Ber")
            waitForIdle()
            onNodeWithTag(userAdminGeofenceAreaTag(2)).performClick()
            onNodeWithText(labels.save).performClick()
            waitUntil(timeoutMillis = 5_000) { saved != null }
        }

        assertEquals(listOf(2L), saved)
    }
}
