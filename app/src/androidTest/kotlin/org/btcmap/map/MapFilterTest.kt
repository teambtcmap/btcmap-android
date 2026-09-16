package org.btcmap.map

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.search.SearchBar
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.settings.SettingsFragment
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntilOnMain
import org.btcmap.view.IconButton
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapView

@RunWith(AndroidJUnit4::class)
class MapFilterTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun filter_isPreserved_whenReturningFromSettings() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var activity: Activity
                scenario.onActivity { activity = it }

                awaitMap(activity)

                click(activity, R.id.showEvents)
                waitUntilOnMain { selectedFilter(activity) == R.id.showEvents }

                navigateToSettings(activity)
                waitUntilOnMain {
                    activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) is SettingsFragment
                }

                activity.runOnUiThread { activity.supportFragmentManager.popBackStack() }
                waitUntilOnMain {
                    activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) is MapFragment
                }

                awaitMap(activity)
                awaitFilter(activity, R.id.showEvents)

                Assert.assertEquals(
                    "Filter should stay on events after returning from settings",
                    R.id.showEvents,
                    selectedFilter(activity),
                )
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun filter_defaultsToMerchants_onColdStart() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var activity: Activity
                scenario.onActivity { activity = it }

                awaitMap(activity)
                click(activity, R.id.showEvents)
                waitUntilOnMain { selectedFilter(activity) == R.id.showEvents }
            }

            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var activity: Activity
                scenario.onActivity { activity = it }

                awaitMap(activity)
                awaitFilter(activity, R.id.showMerchants)

                Assert.assertEquals(
                    "A cold start should default to merchants",
                    R.id.showMerchants,
                    selectedFilter(activity),
                )
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun awaitMap(activity: Activity) {
        waitUntilOnMain {
            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) as? MapFragment
                ?: return@waitUntilOnMain false
            val mapView = fragment.view?.findViewById<MapView>(R.id.map)
                ?: return@waitUntilOnMain false

            var ready = false
            mapView.getMapAsync { ready = true }
            ready
        }
    }

    private fun awaitFilter(activity: Activity, viewId: Int) {
        waitUntilOnMain(timeoutMs = 5_000) { selectedFilter(activity) == viewId }
    }

    private fun selectedFilter(activity: Activity): Int? {
        val fragment = activity.supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as? MapFragment
            ?: return null
        val view = fragment.view ?: return null

        return when {
            view.findViewById<IconButton>(R.id.showMerchants).isSelected -> R.id.showMerchants
            view.findViewById<IconButton>(R.id.showEvents).isSelected -> R.id.showEvents
            view.findViewById<IconButton>(R.id.showExchanges).isSelected -> R.id.showExchanges
            else -> null
        }
    }

    private fun click(activity: Activity, viewId: Int) {
        activity.runOnUiThread { activity.findViewById<IconButton>(viewId).performClick() }
    }

    private fun navigateToSettings(activity: Activity) {
        activity.runOnUiThread {
            val searchBar = activity.findViewById<SearchBar>(R.id.search_bar)
            searchBar.menu.performIdentifierAction(R.id.settings, 0)
        }
    }

    companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
