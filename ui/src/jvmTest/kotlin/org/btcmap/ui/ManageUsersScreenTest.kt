package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The manage-users screen: the prompt before anything is typed, a debounced
 * search that renders the users it returns, the no-match state, and the failure
 * state with its retry action.
 */
class ManageUsersScreenTest {

    private val labels = ManageUsersLabels(
        search = "Search users",
        clear = "Clear search",
        prompt = "Search for a user by name",
        noMatches = "No matching users",
        failed = "Couldn't load the users",
        retry = "Retry",
        created = { "Joined $it" },
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun showsThePromptBeforeTyping() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManageUsersScreen(labels = labels, search = { emptyList() })
                }
            }
            onNodeWithText("Search for a user by name").assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchRendersMatchingUsers() {
        val queries = mutableListOf<String>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManageUsersScreen(
                        labels = labels,
                        search = { query ->
                            queries += query
                            listOf(
                                ManageUserUi(1, "satoshi", listOf("user"), "2024-01-01T00:00:00Z"),
                                ManageUserUi(2, "nat", listOf("admin", "root"), "2024-02-02T00:00:00Z"),
                            )
                        },
                    )
                }
            }
            onNodeWithTag(MANAGE_USERS_SEARCH_TAG).performTextInput("sat")
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("satoshi").fetchSemanticsNodes().isNotEmpty()
            }
            // The query was trimmed and used once; the roles render capitalised
            // and each account shows its creation date.
            onNodeWithText("User").assertExists()
            onNodeWithText("Admin, Root").assertExists()
            onNodeWithText("Joined 2024-01-01T00:00:00Z").assertExists()
            onNodeWithText("Joined 2024-02-02T00:00:00Z").assertExists()
        }
        assertEquals(listOf("sat"), queries)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingAResult_opensTheRecord() {
        var opened: ManageUserUi? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManageUsersScreen(
                        labels = labels,
                        search = {
                            listOf(ManageUserUi(7, "satoshi", listOf("user"), "2024-01-01T00:00:00Z"))
                        },
                        onUserClick = { opened = it },
                    )
                }
            }
            onNodeWithTag(MANAGE_USERS_SEARCH_TAG).performTextInput("sat")
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("satoshi").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("satoshi").performClick()
        }
        assertEquals(7L, opened?.id)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun showsNoMatchesForAnEmptyResult() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManageUsersScreen(labels = labels, search = { emptyList() })
                }
            }
            onNodeWithTag(MANAGE_USERS_SEARCH_TAG).performTextInput("zzz")
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No matching users").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchFailure_showsTheRetryAction() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManageUsersScreen(labels = labels, search = { error("could not reach the server") })
                }
            }
            onNodeWithTag(MANAGE_USERS_SEARCH_TAG).performTextInput("zzz")
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Couldn't load the users").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").assertIsDisplayed()
        }
    }
}
