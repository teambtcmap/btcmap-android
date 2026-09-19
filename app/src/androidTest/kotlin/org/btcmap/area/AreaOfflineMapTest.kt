package org.btcmap.area

import android.view.View
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.R
import org.btcmap.util.waitUntilOnMain
import org.hamcrest.Matchers.containsString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AreaOfflineMapTest : AreaScreenTest() {

    @Test
    fun areaWithBoundingBox_showsToolbarDownloadAndHidesPanel() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).check(matches(isDisplayed()))
            Assert.assertFalse(area.requireView().findViewById<View>(R.id.offline_map).isVisible)
        }
    }

    @Test
    fun areaWithoutBoundingBox_hidesToolbarDownload() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(bboxWest = null, bboxSouth = null, bboxEast = null, bboxNorth = null)),
        )

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).check(doesNotExist())
            Assert.assertFalse(area.requireView().findViewById<View>(R.id.offline_map).isVisible)
        }
    }

    @Test
    fun toolbarDownload_opensDialogWithZoomSelectionAndEstimate() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).perform(click())

            onView(withId(R.id.zoom)).inRoot(isDialog())
                .check(matches(withText(containsString("Maximum zoom"))))
            onView(withId(R.id.zoom_slider)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withId(R.id.estimate)).inRoot(isDialog())
                .check(matches(withText(containsString("Estimated size"))))
        }
    }
}
