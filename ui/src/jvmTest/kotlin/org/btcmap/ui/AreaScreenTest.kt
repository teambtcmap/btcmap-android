package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The area body caps and centres on a wide window so its cards and text do not
 * stretch edge to edge, while the host still owns the scroll.
 */
@OptIn(ExperimentalTestApi::class)
class AreaScreenTest {

    @Test
    fun wideWindow_capsAndCentresTheBody() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    Box(Modifier.requiredSize(1000.dp, 800.dp)) {
                        AreaScreen(
                            description = "A place to spend sats.",
                            websiteText = null,
                            boostedMerchants = emptyList(),
                            events = emptyList(),
                            issues = null,
                            offlineState = null,
                            offlineStyleMatches = { true },
                            strings = strings(),
                            onOpenPlace = {},
                            onOpenEvent = {},
                            onOpenIssue = {},
                            onJoinUs = {},
                            onDownload = {},
                            onDelete = {},
                            headerImageUrl = "file:///nonexistent.png",
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
            val bounds = onNodeWithTag(AREA_HEADER_TAG).getBoundsInRoot()
            // The body is capped at CONTENT_MAX_WIDTH and centred in the 1000dp
            // canvas; the header spans the capped width minus the 16dp margins.
            assertEquals(568f, bounds.right.value - bounds.left.value, 2f)
            assertEquals(216f, bounds.left.value, 2f)
        }
    }

    private fun strings() = AreaStrings(
        readMore = "Read more",
        collapse = "Collapse",
        boostedMerchants = "Boosted merchants",
        events = "Events",
        howToHelp = "How to help",
        offlineMap = "Offline map",
        offlineDownload = "Download",
        offlineDownloadAgain = "Download again",
        offlineDelete = "Delete",
        cancel = "Cancel",
        boosted = "Boosted",
        boostedUntil = { "Boosted until $it" },
        issues = { shown, total -> "$shown of $total issues" },
        issueDescription = { it },
        offlineStatusDownloading = { "Downloading $it" },
        offlineStatusProgress = { percent, size -> "$percent% of $size" },
        offlineStatusDownloaded = { size, min, max -> "$size ($min-$max)" },
        offlineStyleMismatch = "Style mismatch",
        offlineStatusFailed = { it },
        offlineDialogDescription = { it },
        offlineDialogStyle = { it },
        offlineDialogMaxZoom = { "Zoom $it" },
        offlineDialogEstimatedSize = { it },
        offlineDialogTooLarge = { it },
        offlineDialogEstimateNote = "Estimate",
        formatBytes = { it.toString() },
    )
}
