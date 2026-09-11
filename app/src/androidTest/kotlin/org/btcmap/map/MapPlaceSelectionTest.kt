package org.btcmap.map

import android.graphics.RectF
import android.view.InputDevice
import android.view.MotionEvent
import androidx.appcompat.widget.Toolbar
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
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.map.layer.MERCHANT_MARKER_ICON_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_OUTDATED_LAYER_ID
import org.btcmap.place.PlaceFragment
import org.btcmap.settings.mapViewport
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
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
class MapPlaceSelectionTest {

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
    fun bundledPlacesInParis_renderOnMap_andClickOpensBottomSheet() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            renderSelectAndAssert()
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun renderSelectAndAssert() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var activity: Activity
            lateinit var mapFragment: MapFragment
            lateinit var mapView: MapView
            var map: MapLibreMap? = null
            var renderedIds: Set<Long> = emptySet()

            scenario.onActivity {
                activity = it
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                mapView = mapFragment.requireView().findViewById(R.id.map)
                mapView.getMapAsync { m -> map = m }
            }

            waitUntilOnMain {
                val currentMap = map ?: return@waitUntilOnMain false
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
            MERCHANT_MARKER_OUTDATED_LAYER_ID,
            MERCHANT_MARKER_ICON_LAYER_ID,
        ).mapNotNull { it.getProperty("id")?.asLong }.toSet()
    }

    private fun clickOn(map: MapLibreMap, place: Place) {
        val coordinates = CoordinatesProvider { view ->
            val point = map.projection.toScreenLocation(LatLng(place.lat, place.lon))
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            floatArrayOf(location[0] + point.x, location[1] + point.y)
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
    }
}
