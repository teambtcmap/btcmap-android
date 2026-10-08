package org.btcmap.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.db.table.place.Place
import kotlin.time.Instant

/**
 * The place body's photo row: the strip always draws at the same height, with
 * the squared add tile last, so photos arriving after the sheet opens do not
 * change the row's height.
 */
@OptIn(ExperimentalTestApi::class)
class PlaceSheetTest {

    @Test
    fun withoutPhotos_showsPlaceholderAndAddTile() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = emptyList(),
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                    )
                }
            }
            onNodeWithTag(PLACE_PHOTO_PLACEHOLDER_TAG).assertExists()
            onNodeWithTag(PLACE_ADD_PHOTO_TAG).assertExists()
            onAllNodesWithTag(PLACE_PHOTO_TAG_PREFIX + 0).assertCountEquals(0)
        }
    }

    @Test
    fun withPhotos_hidesThePlaceholderAndShowsTheStrip() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = listOf(photo(0)),
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                    )
                }
            }
            onNodeWithTag(PLACE_PHOTO_TAG_PREFIX + 0).assertExists()
            // The add affordance stays as the strip's trailing tile.
            onNodeWithTag(PLACE_ADD_PHOTO_TAG).assertExists()
            // The stand-in goes away once there are real photos.
            onNodeWithTag(PLACE_PHOTO_PLACEHOLDER_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun addingPhoto_disablesTheAddTileAndShowsALoader() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = emptyList(),
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                        addingPhoto = true,
                    )
                }
            }
            onNodeWithTag(PLACE_ADD_PHOTO_TAG).assertIsNotEnabled()
            // The spinner replaces the add glyph while the upload runs.
            onNodeWithContentDescription("Add photo").assertDoesNotExist()
        }
    }

    @Test
    fun photosArriving_appearBeforeTheAddTile() {
        val photos = mutableStateOf(emptyList<PlacePhoto>())
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = photos.value,
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                    )
                }
            }
            onNodeWithTag(PLACE_PHOTO_TAG_PREFIX + 0).assertDoesNotExist()
            onNodeWithTag(PLACE_PHOTO_PLACEHOLDER_TAG).assertExists()

            runOnIdle { photos.value = listOf(photo(0)) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(PLACE_PHOTO_TAG_PREFIX + 0).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(PLACE_PHOTO_PLACEHOLDER_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun headerActions_offerDirectionsAndShare() {
        val actions = mutableListOf<PlaceAction>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = emptyList(),
                        bookmarked = false,
                        strings = strings(),
                        onAction = { actions += it },
                    )
                }
            }
            onNodeWithTag(PLACE_DIRECTIONS_TAG).performClick()
            onNodeWithTag(PLACE_SHARE_TAG).performClick()
        }
        assertEquals(listOf(PlaceAction.Directions, PlaceAction.Share), actions)
    }

    @Test
    fun inlineOverflow_revealsTheItemsUnderTheHeader() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = emptyList(),
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                        overflowInline = true,
                    )
                }
            }
            onNodeWithText("View on BTC Map").assertDoesNotExist()
            onNodeWithTag(PLACE_OVERFLOW_TAG).performClick()
            onNodeWithText("View on BTC Map").assertIsDisplayed()
        }
    }

    @Test
    fun share_showsTheConfirmationUnderTheHeader() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PlaceDetails(
                        place = place(),
                        comments = emptyList(),
                        photos = emptyList(),
                        bookmarked = false,
                        strings = strings(),
                        onAction = {},
                        shareConfirmation = "Place link copied to clipboard",
                    )
                }
            }
            onNodeWithTag(PLACE_SHARE_CONFIRMATION_TAG).assertDoesNotExist()
            onNodeWithTag(PLACE_SHARE_TAG).performClick()
            onNodeWithTag(PLACE_SHARE_CONFIRMATION_TAG).assertIsDisplayed()
        }
    }

    private fun photo(index: Long) = PlacePhoto(
        imageId = index,
        placeId = PLACE_ID,
        thumbnailUrl = "https://example.invalid/$index.jpg",
        fullUrl = "https://example.invalid/$index-full.jpg",
    )

    private fun place() = Place(
        id = PLACE_ID,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        lat = 0.0,
        lon = 0.0,
        icon = "storefront",
        name = "Kart Cafe",
        localizedName = null,
        verifiedAt = null,
        address = null,
        openingHours = null,
        phone = null,
        website = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
        requiredAppUrl = null,
        boostedUntil = null,
        comments = null,
        telegram = null,
        osmId = null,
    )

    private fun strings() = PlaceSheetStrings(
        directions = "Directions",
        share = "Share",
        viewOnBtcmap = "View on BTC Map",
        viewOnOsm = "View on OSM",
        editOnOsm = "Edit on OSM",
        notVerified = "Not verified",
        verificationWarningTitle = "Verification",
        verificationWarningOutdated = "Outdated",
        verificationWarningNotVerified = "Not verified",
        ok = "OK",
        companionWarning = { "Requires $it" },
        verify = "Verify",
        report = "Report",
        boost = "Boost",
        commentsTitle = { "Comments ($it)" },
        addComment = "Add comment",
        watch = "Watch",
        unwatch = "Unwatch",
        addPhoto = "Add photo",
        uploadedBy = { "Uploaded by $it" },
        deletePhoto = "Delete photo",
        openingHoursClosed = "Closed",
        openingHoursOpen24_7 = "24/7",
    )

    private companion object {
        const val PLACE_ID = 7L
    }
}
