package org.btcmap.area

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AreaLoadErrorHandlingTest : AreaScreenTest() {

    @Test
    fun loadSuccess_hidesLoadingIndicatorAndShowsContent() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { scenario, area ->
            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }

            scenario.onActivity {
                Assert.assertTrue(
                    "Content should be visible",
                    area.requireView().findViewById<View>(R.id.content).isVisible,
                )
                Assert.assertEquals(
                    "Grand Paris",
                    area.requireView().findViewById<Toolbar>(R.id.toolbar).title,
                )
            }
        }
    }

    @Test
    fun missingCachedArea_showsErrorDialogAndClosesScreenOnDismiss() {
        withArea(addToBackStack = true) { scenario, area ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntil {
                try {
                    onView(withText(android.R.string.ok)).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(AREA_TAG) == null
            }
        }
    }

    @Test
    fun cachedAreaWithoutGeometry_showsContent() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(
                area(
                    bboxWest = null,
                    bboxSouth = null,
                    bboxEast = null,
                    bboxNorth = null,
                    geoJson = null,
                ),
            ),
        )

        withArea { scenario, area ->
            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }

            scenario.onActivity {
                Assert.assertTrue(
                    "Cached area should keep content visible",
                    area.requireView().findViewById<View>(R.id.content).isVisible,
                )
                Assert.assertEquals(
                    "Grand Paris",
                    area.requireView().findViewById<Toolbar>(R.id.toolbar).title,
                )
            }
        }
    }

    @Test
    fun issuesFailure_keepsContentVisibleWithoutErrorDialog() {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = """{"message":"boom"}""",
            issuesCode = 500,
        )
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.event.insert(
            listOf(event(id = 1, name = "Meetup", startsAt = "2999-01-01T10:00:00Z")),
        )

        withArea { _, area ->
            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }
            waitUntilOnMain {
                area.requireView()
                    .findViewById<ViewGroup>(R.id.upcoming_events_container)
                    .childCount == 1
            }

            Assert.assertTrue(
                area.requireView().findViewById<View>(R.id.content).isVisible,
            )
            // The issues failure must not interrupt the cached screen with an
            // error dialog: there is no dialog root to match.
            val dialogShown = try {
                onView(withText(android.R.string.ok)).inRoot(isDialog())
                    .check(matches(isDisplayed()))
                true
            } catch (e: NoMatchingRootException) {
                false
            }
            Assert.assertFalse("issues failure must not show an error dialog", dialogShown)
        }
    }
}
