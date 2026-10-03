package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import org.btcmap.place.MAX_REPORT_PHOTOS

/**
 * The desktop report form: a reason must be chosen before it can be submitted,
 * and the evidence photos can be attached and removed up to the cap.
 */
class DesktopReportScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun submit_staysDisabledUntilAReasonIsPicked() {
        runComposeUiTest {
            setContent {
                DesktopReportScreen(
                    api = testApi(),
                    placeId = 1L,
                    placeName = "Cafe",
                    initialType = null,
                    onBack = {},
                )
            }
            onNodeWithText("Verified - still accepts Bitcoin").assertIsDisplayed()
            onNodeWithText("Submit").assertIsNotEnabled()
            // The three reasons are the only selectable nodes, in form order.
            onAllNodes(isSelectable())[1].performClick()
            onNodeWithText("Submit").assertIsEnabled()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun addedPhoto_showsThumbnailAndCanBeRemoved() {
        runComposeUiTest {
            setContent {
                DesktopReportScreen(
                    api = testApi(),
                    placeId = 1L,
                    placeName = "Cafe",
                    initialType = "verified",
                    onBack = {},
                    pickPhotos = { listOf(pngBytes()) },
                )
            }
            onNodeWithTag(REPORT_ADD_PHOTO_TAG).performScrollTo().performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(REPORT_PHOTO_TAG_PREFIX + 0).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(REPORT_ADD_PHOTO_TAG).assertTextContains("Add photo (1/$MAX_REPORT_PHOTOS)")

            onNodeWithTag(REPORT_PHOTO_REMOVE_TAG_PREFIX + 0).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(REPORT_PHOTO_TAG_PREFIX + 0).fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun photoLimit_isEnforced() {
        runComposeUiTest {
            setContent {
                DesktopReportScreen(
                    api = testApi(),
                    placeId = 1L,
                    placeName = "Cafe",
                    initialType = "verified",
                    onBack = {},
                    pickPhotos = { List(MAX_REPORT_PHOTOS + 3) { pngBytes() } },
                )
            }
            onNodeWithTag(REPORT_ADD_PHOTO_TAG).performScrollTo().performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(REPORT_PHOTO_TAG_PREFIX + (MAX_REPORT_PHOTOS - 1))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            // The extra picks beyond the cap are dropped.
            onAllNodesWithTag(REPORT_PHOTO_TAG_PREFIX + MAX_REPORT_PHOTOS).assertCountEquals(0)
        }
    }

    /** A tiny valid PNG, so the thumbnail decoder has a real image to read. */
    private fun pngBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out)
        return out.toByteArray()
    }
}
