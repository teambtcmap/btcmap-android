package org.btcmap.map

import android.graphics.RectF
import android.view.InputDevice
import android.view.MotionEvent
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.place.PlaceFragment
import org.btcmap.search.SearchAdapter
import org.btcmap.search.SearchAdapterItem
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.mapViewport
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@RunWith(AndroidJUnit4::class)
class MapPlaceSelectionTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun placesInParis_renderOnMap_andClickOpensBottomSheet() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            renderSelectAndAssert()
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun selectedPlace_survivesRecreation() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var mapFragment: MapFragment
            lateinit var results: RecyclerView

            scenario.onActivity {
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                results = mapFragment.requireView().findViewById(R.id.searchResults)
                val adapter = results.adapter as SearchAdapter
                mapFragment.requireView()
                    .findViewById<SearchView>(R.id.searchView)
                    .show()
                adapter.submitList(
                    listOf(
                        SearchAdapterItem.Place(
                            placeId = ParisPlaces.target.id,
                            icon = ParisPlaces.target.icon,
                            name = ParisPlaces.target.name.orEmpty(),
                            distanceToUser = null,
                            boosted = false,
                        )
                    )
                )
            }

            waitUntilOnMain { results.childCount == 1 }
            scenario.onActivity { results.getChildAt(0).performClick() }
            waitUntilOnMain {
                sheetState(mapFragment) == BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            scenario.recreate()

            scenario.onActivity {
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
            }
            waitUntilOnMain {
                sheetState(mapFragment) == BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            val placeFragment = mapFragment.childFragmentManager
                .findFragmentById(R.id.placeFragment) as PlaceFragment
            val title = placeFragment.requireView()
                .findViewById<Toolbar>(R.id.toolbar).title

            Assert.assertEquals(ParisPlaces.target.name, title)
        }
    }

    @Test
    fun selectedPlace_survivesReturningFromSettings() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var activity: Activity
            lateinit var mapFragment: MapFragment
            lateinit var results: RecyclerView

            scenario.onActivity {
                activity = it
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                results = mapFragment.requireView().findViewById(R.id.searchResults)
                val adapter = results.adapter as SearchAdapter
                mapFragment.requireView()
                    .findViewById<SearchView>(R.id.searchView)
                    .show()
                adapter.submitList(
                    listOf(
                        SearchAdapterItem.Place(
                            placeId = ParisPlaces.target.id,
                            icon = ParisPlaces.target.icon,
                            name = ParisPlaces.target.name.orEmpty(),
                            distanceToUser = null,
                            boosted = false,
                        )
                    )
                )
            }

            waitUntilOnMain { results.childCount == 1 }
            scenario.onActivity { results.getChildAt(0).performClick() }
            waitUntilOnMain {
                sheetState(mapFragment) == BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            activity.runOnUiThread {
                activity.findViewById<SearchBar>(R.id.search_bar)
                    .menu.performIdentifierAction(R.id.settings, 0)
            }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is SettingsFragment
            }

            activity.runOnUiThread { activity.supportFragmentManager.popBackStack() }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is MapFragment
            }

            // The view is recreated without a saved instance state, so the sheet
            // has to be rebuilt from the fragment's retained fields.
            waitUntilOnMain {
                mapFragment.view != null &&
                    sheetState(mapFragment) == BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            val placeFragment = mapFragment.childFragmentManager
                .findFragmentById(R.id.placeFragment) as PlaceFragment
            val title = placeFragment.requireView()
                .findViewById<Toolbar>(R.id.toolbar).title

            Assert.assertEquals(ParisPlaces.target.name, title)
        }
    }

    private fun sheetState(fragment: MapFragment): Int {
        return BottomSheetBehavior.from(
            fragment.requireView().findViewById(R.id.placeBottomSheet)
        ).state
    }

    private fun renderSelectAndAssert() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var activity: Activity
            lateinit var mapFragment: MapFragment
            lateinit var mapView: MapView
            var map: MapLibreMap? = null
            var mapFinishedLoading = false
            var renderedIds: Set<Long> = emptySet()

            scenario.onActivity {
                activity = it
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                mapView = mapFragment.requireView().findViewById(R.id.map)
                mapView.addOnDidFinishLoadingMapListener { mapFinishedLoading = true }
                mapView.getMapAsync { m -> map = m }
            }

            waitUntilOnMain {
                // queryRenderedFeatures crashes in native code until the renderer
                // has actually started, so wait for the map to finish loading.
                if (!mapFinishedLoading) return@waitUntilOnMain false
                val currentMap = map ?: return@waitUntilOnMain false
                val style = currentMap.style ?: return@waitUntilOnMain false
                if (style.getLayer(MERCHANT_MARKER_LAYER_ID) == null) {
                    return@waitUntilOnMain false
                }
                renderedIds = renderedPlaceIds(currentMap, mapView)
                renderedIds.size == ParisPlaces.places.size
            }

            Assert.assertEquals(
                ParisPlaces.places.map { it.id }.toSet(),
                renderedIds,
            )

            clickOn(map!!, ParisPlaces.target)

            waitUntilOnMain {
                bottomSheetBehavior(activity).state == BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            val placeFragment = mapFragment.childFragmentManager
                .findFragmentById(R.id.placeFragment) as PlaceFragment
            val title = placeFragment.requireView().findViewById<Toolbar>(R.id.toolbar).title

            Assert.assertEquals(ParisPlaces.target.name, title)

            waitUntil { apiRule.server.requestCount > 0 }

            Assert.assertEquals(
                ParisPlaces.places.size.toLong(),
                databaseRule.db.place.selectCount(),
            )
        }
    }

    private fun renderedPlaceIds(map: MapLibreMap, mapView: MapView): Set<Long> {
        val rect = RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat())
        return map.queryRenderedFeatures(
            rect,
            MERCHANT_MARKER_LAYER_ID,
        ).mapNotNull { it.getProperty("id")?.asLong }.toSet()
    }

    private fun clickOn(map: MapLibreMap, place: Place) {
        val coordinates = CoordinatesProvider { view ->
            val point = map.projection.toScreenLocation(LatLng(place.lat, place.lon))
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            // Markers are anchored at the pin tip, so tap the body above it.
            val pinCenterOffset = MARKER_PIN_HEIGHT_DP / 2f * view.resources.displayMetrics.density
            floatArrayOf(location[0] + point.x, location[1] + point.y - pinCenterOffset)
        }

        onView(allOf(withId(R.id.map), isDisplayed())).perform(
            GeneralClickAction(
                Tap.SINGLE,
                coordinates,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY,
            )
        )
    }

    private fun bottomSheetBehavior(activity: Activity): BottomSheetBehavior<*> {
        return BottomSheetBehavior.from(activity.findViewById(R.id.placeBottomSheet))
    }

    companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
        private const val MARKER_PIN_HEIGHT_DP = 48f
    }
}
