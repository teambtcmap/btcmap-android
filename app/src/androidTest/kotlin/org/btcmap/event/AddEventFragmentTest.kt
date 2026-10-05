package org.btcmap.event

import android.os.Bundle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.ui.ADD_EVENT_NAME_TAG
import org.btcmap.ui.ADD_EVENT_SUBMIT_TAG
import org.btcmap.util.AppTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the add-event screen, which renders the shared Compose form.
 */
@RunWith(AndroidJUnit4::class)
class AddEventFragmentTest : AppTestCase() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun addEvent_showsTheForm() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            scenario.onActivity { addFragment(it) }
            waitForForm()
            composeTestRule.onNodeWithTag(ADD_EVENT_NAME_TAG).assertExists()
        }
    }

    @Test
    fun recreation_keepsTheForm() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            scenario.onActivity { addFragment(it) }
            waitForForm()

            scenario.recreate()

            waitForForm()
            composeTestRule.onNodeWithTag(ADD_EVENT_NAME_TAG).assertExists()
        }
    }

    @Test
    fun blankForm_showsRequiredErrors() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            scenario.onActivity { addFragment(it) }
            waitForForm()

            composeTestRule.onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()

            composeTestRule.onAllNodesWithText("Required").assertCountEquals(2)
        }
    }

    private fun waitForForm() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(ADD_EVENT_NAME_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun addFragment(activity: Activity): AddEventFragment {
        val fragment = newFragment()
        activity.supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, fragment, TAG)
        }
        return fragment
    }

    private fun newFragment(): AddEventFragment = AddEventFragment().apply {
        arguments = Bundle().apply {
            putDouble("lat", 1.0)
            putDouble("lon", 2.0)
        }
    }

    private companion object {
        const val TAG = "add-event"
    }
}
