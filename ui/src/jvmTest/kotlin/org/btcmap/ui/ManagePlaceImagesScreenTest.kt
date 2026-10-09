package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The manage-place-images screen: the newest uploads render with their place,
 * uploader and date, a delete removes the row it belongs to, a row opens the
 * full-screen viewer, and the empty and failure states are shown.
 */
class ManagePlaceImagesScreenTest {

    private val labels = ManagePlaceImagesLabels(
        empty = "No place images",
        failed = "Couldn't load the place images",
        retry = "Retry",
        unknownPlace = { "Place #$it" },
        unknownUploader = "Unknown uploader",
        by = { "by $it" },
        date = { "date:$it" },
        fullscreen = "Full-screen image",
        delete = DeleteActionLabels(
            delete = "Delete",
            title = "Delete this image?",
            message = "The image will be removed for everyone.",
            failed = "Couldn't delete the image",
            cancel = "Cancel",
        ),
    )

    private fun image(
        placeId: Long,
        imageId: Long,
        placeName: String,
        uploaderName: String?,
        createdAt: String = "2026-10-01T04:22:53.706Z",
    ) = ManagePlaceImageUi(
        placeId = placeId,
        imageId = imageId,
        thumbnailUrl = "https://example.invalid/$imageId-thumb.jpg",
        fullUrl = "https://example.invalid/$imageId-full.jpg",
        placeName = placeName,
        uploaderName = uploaderName,
        createdAt = createdAt,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun listsImagesWithUploaderAndDate() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManagePlaceImagesScreen(
                        labels = labels,
                        load = {
                            listOf(
                                image(42, 3, "Cafe", "satoshi", createdAt = "2026-10-03T04:22:53.706Z"),
                                image(43, 4, "Bar", null, createdAt = "2026-10-04T04:22:53.706Z"),
                            )
                        },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Cafe").assertExists()
            onNodeWithText("by satoshi").assertExists()
            onNodeWithText("Unknown uploader").assertExists()
            onNodeWithText("date:2026-10-03T04:22:53.706Z").assertExists()
            onNodeWithText("date:2026-10-04T04:22:53.706Z").assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingRow_opensTheFullscreenViewer() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManagePlaceImagesScreen(
                        labels = labels,
                        load = { listOf(image(42, 3, "Cafe", "satoshi")) },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Cafe").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Cafe").performClick()
            onNodeWithContentDescription("Full-screen image").assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deletesOneAfterConfirming() {
        val deleted = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManagePlaceImagesScreen(
                        labels = labels,
                        load = {
                            listOf(
                                image(42, 3, "Cafe", "satoshi"),
                                image(43, 4, "Bar", null),
                            )
                        },
                        delete = { deleted += it.imageId },
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bar").fetchSemanticsNodes().isNotEmpty()
            }

            // The delete opens a confirmation; only after confirming does the
            // row go and the API call run.
            onNodeWithTag(MANAGE_PLACE_IMAGES_DELETE_TAG_PREFIX + 4L).performClick()
            onNodeWithText("Delete").performClick()
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
                    ManagePlaceImagesScreen(
                        labels = labels,
                        load = { emptyList() },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No place images").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadFailure_showsTheRetryAction() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    ManagePlaceImagesScreen(
                        labels = labels,
                        load = { error("could not reach the server") },
                        delete = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Couldn't load the place images").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").assertIsDisplayed()
        }
    }
}
