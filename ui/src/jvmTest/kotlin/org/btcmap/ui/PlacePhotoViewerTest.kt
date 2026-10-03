package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/** The full-screen viewer shows the delete action only for deletable photos. */
class PlacePhotoViewerTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteButton_shownOnlyWhenCanDelete() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlacePhotoViewer(
                        photos = listOf(
                            PlacePhoto(
                                imageId = 3,
                                placeId = 42,
                                thumbnailUrl = "https://example.invalid/t.jpg",
                                fullUrl = "https://example.invalid/f.jpg",
                                authorName = "satoshi",
                                canDelete = true,
                            ),
                        ),
                        initialIndex = 0,
                        uploadedBy = { "Uploaded by $it" },
                        deleteDescription = "Delete photo",
                        onDelete = {},
                        onDismiss = {},
                    )
                }
            }
            onNodeWithTag(PLACE_PHOTO_DELETE_TAG).assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteButton_hiddenWhenNotAllowed() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlacePhotoViewer(
                        photos = listOf(
                            PlacePhoto(
                                imageId = 3,
                                placeId = 42,
                                thumbnailUrl = "https://example.invalid/t.jpg",
                                fullUrl = "https://example.invalid/f.jpg",
                                authorName = "satoshi",
                                canDelete = false,
                            ),
                        ),
                        initialIndex = 0,
                        uploadedBy = { "Uploaded by $it" },
                        deleteDescription = "Delete photo",
                        onDelete = {},
                        onDismiss = {},
                    )
                }
            }
            onAllNodesWithTag(PLACE_PHOTO_DELETE_TAG).assertCountEquals(0)
        }
    }
}
