package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The saved-items screen: the account's saved places or areas render, a delete
 * removes the row it belongs to, an account with nothing saved sees the empty
 * state, and a wide window caps and centres the list.
 */
class SavedItemsScreenTest {

    private val labels = SavedItemsLabels(
        empty = "No saved places",
        delete = "Delete",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun listsItemsAndDeletesOne() {
        val stored = mutableListOf(
            SavedItemUi(10, "Cafe"),
            SavedItemUi(11, "Bar"),
        )
        val deleted = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    SavedItemsScreen(
                        labels = labels,
                        load = { stored.toList() },
                        delete = {
                            deleted += it.id
                            stored.removeAll { item -> item.id == it.id }
                        },
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Cafe").assertExists()

            onNodeWithTag(SAVED_ITEMS_DELETE_TAG_PREFIX + 11L).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isEmpty()
            }
            // Only the deleted row goes; the other stays.
            onNodeWithText("Cafe").assertExists()
        }
        assertEquals(listOf(11L), deleted)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun showsTheEmptyState() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    SavedItemsScreen(
                        labels = labels,
                        load = { emptyList() },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No saved places").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun wideWindow_capsAndCentresTheList() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    Box(Modifier.requiredSize(1000.dp, 800.dp)) {
                        SavedItemsScreen(
                            labels = labels,
                            load = { listOf(SavedItemUi(10, "Cafe")) },
                            delete = {},
                        )
                    }
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Cafe").fetchSemanticsNodes().isNotEmpty()
            }
            val bounds = onNodeWithText("Cafe").getBoundsInRoot()
            // Capped at CONTENT_MAX_WIDTH and centred in the 1000dp canvas, so
            // the text starts at the capped body's left edge plus its 16dp
            // padding: (1000 - 600) / 2 + 16 = 216.
            assertEquals(216f, bounds.left.value, 2f)
        }
    }
}
