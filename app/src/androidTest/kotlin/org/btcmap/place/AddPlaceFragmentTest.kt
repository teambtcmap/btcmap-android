package org.btcmap.place

import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.util.AppTestCase
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Drives the add-place screen, which owns a map of its own and otherwise has no
 * coverage. Besides pinning the basic form, these tests cover the map's
 * asynchronous lifecycle: the map is created off the view's creation and its
 * callbacks must not touch the context once the view is gone.
 */
@RunWith(AndroidJUnit4::class)
class AddPlaceFragmentTest : AppTestCase() {

    @Test
    fun addPlace_showsTheForm() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: AddPlaceFragment
            scenario.onActivity { fragment = addFragment(it) }

            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_submit).isShown
            }
        }
    }

    @Test
    fun closingBeforeTheMapIsReady_doesNotLeakUncaughtException() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            assertNoUncaughtException(
                "Add-place map callback touched a view that was already gone",
            ) {
                scenario.onActivity { activity ->
                    val fragment = newFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, fragment, TAG)
                    }
                    // Tear the screen down before the map's asynchronous ready
                    // callback can run, the case the callback guards.
                    activity.supportFragmentManager.commitNow { remove(fragment) }
                }
            }
        }
    }

    @Test
    fun addPlace_doesNotInstallTheUnusedMarkerImage() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            var map: MapLibreMap? = null
            scenario.onActivity { activity ->
                val fragment = addFragment(activity)
                fragment.requireView().findViewById<MapView>(R.id.map).getMapAsync { map = it }
            }

            waitUntilOnMain { map?.style?.isFullyLoaded == true }

            // The screen has no marker layer of its own: the pin is a view
            // overlay, so the map style must stay free of the merchant marker
            // image the main map installs. Style reads have to run on the UI
            // thread.
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Assert.assertNull(map!!.style!!.getImage(MERCHANT_MARKER_IMAGE))
            }
        }
    }

    @Test
    fun recreation_keepsTheForm() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            scenario.onActivity { addFragment(it) }

            scenario.recreate()

            lateinit var recreated: AddPlaceFragment
            scenario.onActivity { activity ->
                recreated = activity.supportFragmentManager
                    .findFragmentByTag(TAG) as AddPlaceFragment
            }

            Assert.assertTrue(
                recreated.requireView().findViewById<Button>(R.id.btn_submit).isShown,
            )
        }
    }

    private fun addFragment(activity: Activity): AddPlaceFragment {
        val fragment = newFragment()
        activity.supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, fragment, TAG)
        }
        return fragment
    }

    private fun newFragment(): AddPlaceFragment = AddPlaceFragment().apply {
        arguments = Bundle().apply {
            putDouble("lat", 1.0)
            putDouble("lon", 2.0)
        }
    }

    private companion object {
        const val TAG = "add-place"

        /** The image the main map installs for its merchant markers. */
        const val MERCHANT_MARKER_IMAGE = "btcmap-marker"
    }
}
