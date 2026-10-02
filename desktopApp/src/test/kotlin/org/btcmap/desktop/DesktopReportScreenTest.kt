package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/** The desktop report form: a reason must be chosen before it can be submitted. */
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
}
