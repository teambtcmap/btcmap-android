package org.btcmap.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The verify action: the in-flight spinner keeps the icon's centre. */
class AreaAdminVerifyActionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun spinner_isCentredOnTheIcon() {
        runComposeUiTest {
            var verifying by mutableStateOf(false)
            setContent {
                AreaAdminVerifyAction(
                    verifying = verifying,
                    enabled = true,
                    verifyDescription = "Verify",
                    onVerify = {},
                )
            }
            waitForIdle()

            val icon = onNodeWithTag(AREA_ADMIN_VERIFY_BUTTON_TAG)
                .fetchSemanticsNode().boundsInRoot

            verifying = true
            waitForIdle()

            val spinner = onNodeWithTag(AREA_ADMIN_VERIFY_PROGRESS_TAG)
                .fetchSemanticsNode().boundsInRoot

            assertEquals(icon.center.x, spinner.center.x, 0.5f)
            assertEquals(icon.center.y, spinner.center.y, 0.5f)
        }
    }
}
