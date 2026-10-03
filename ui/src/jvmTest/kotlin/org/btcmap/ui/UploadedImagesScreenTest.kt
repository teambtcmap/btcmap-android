package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The uploaded-images screen: the account's photos render, a delete removes the
 * row it belongs to, and an account with no uploads sees the empty state.
 */
class UploadedImagesScreenTest {

    private val labels = UploadedImagesLabels(
        empty = "No uploads",
        delete = "Delete",
        failed = "Couldn't delete the image.",
        retry = "Retry",
        unknownPlace = { "Place #$it" },
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun listsImagesAndDeletesOne() {
        val deleted = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    UploadedImagesScreen(
                        labels = labels,
                        load = {
                            listOf(
                                UploadedImageUi(42, 3, "https://example.invalid/3.jpg", "Cafe"),
                                UploadedImageUi(43, 4, "https://example.invalid/4.jpg", "Bar"),
                            )
                        },
                        delete = { deleted += it.imageId },
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Cafe").assertExists()

            onNodeWithTag(UPLOADED_IMAGES_DELETE_TAG_PREFIX + 4L).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isEmpty()
            }
            // Only the deleted row goes; the other stays.
            onNodeWithText("Cafe").assertExists()
        }
        assertEquals(listOf(4L), deleted)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun showsTheEmptyState() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    UploadedImagesScreen(
                        labels = labels,
                        load = { emptyList() },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No uploads").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
