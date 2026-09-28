package org.btcmap.map

import android.graphics.RectF
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.map.layer.EVENT_MARKER_LAYER_ID
import org.btcmap.map.layer.EXCHANGE_MARKER_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.settings.mapViewport
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.btcmap.view.IconButton
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.time.ZonedDateTime

/**
 * Covers swapping the viewport cache on a filter change: each switch must clear
 * the previous filter's source and draw the new one, including after several
 * switches, and the cache must not be left feeding an old source.
 */
@RunWith(AndroidJUnit4::class)
class MapFilterRenderingTest : AppTestCase() {

    @Test
    fun switchingFilters_showsOnlyTheActiveMarkerType() {
        databaseRule.db.place.insert(ParisPlaces.places)
        databaseRule.db.place.insert(listOf(exchange()))
        databaseRule.db.event.insert(listOf(event()))
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var activity: Activity
            lateinit var mapView: MapView
            var map: MapLibreMap? = null

            scenario.onActivity {
                activity = it
                val fragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                mapView = fragment.requireView().findViewById(R.id.map)
                mapView.getMapAsync { m -> map = m }
            }

            await(map = { map }, mapView = { mapView }) { m, view ->
                rendered(m, view, MERCHANT_MARKER_LAYER_ID) > 0
            }

            click(activity, R.id.showExchanges)
            await(map = { map }, mapView = { mapView }) { m, view ->
                rendered(m, view, EXCHANGE_MARKER_LAYER_ID) > 0 &&
                    rendered(m, view, MERCHANT_MARKER_LAYER_ID) == 0
            }

            click(activity, R.id.showEvents)
            await(map = { map }, mapView = { mapView }) { m, view ->
                rendered(m, view, EVENT_MARKER_LAYER_ID) > 0 &&
                    rendered(m, view, EXCHANGE_MARKER_LAYER_ID) == 0 &&
                    rendered(m, view, MERCHANT_MARKER_LAYER_ID) == 0
            }

            // Back to the first filter after two cache swaps; the merchants must
            // render again rather than staying stale.
            click(activity, R.id.showMerchants)
            await(map = { map }, mapView = { mapView }) { m, view ->
                rendered(m, view, MERCHANT_MARKER_LAYER_ID) > 0 &&
                    rendered(m, view, EVENT_MARKER_LAYER_ID) == 0
            }

            Assert.assertTrue(selected(activity, R.id.showMerchants))
        }
    }

    private fun await(
        map: () -> MapLibreMap?,
        mapView: () -> MapView,
        condition: (MapLibreMap, MapView) -> Boolean,
    ) {
        waitUntilOnMain {
            // queryRenderedFeatures crashes in native code until the renderer
            // has started, so wait for the style to be fully loaded, the same
            // gate production uses before it queries features.
            val currentMap = map() ?: return@waitUntilOnMain false
            val style = currentMap.style ?: return@waitUntilOnMain false
            if (!style.isFullyLoaded) return@waitUntilOnMain false
            condition(currentMap, mapView())
        }
    }

    private fun rendered(map: MapLibreMap, mapView: MapView, layerId: String): Int {
        val rect = RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat())
        return map.queryRenderedFeatures(rect, layerId).size
    }

    private fun click(activity: Activity, viewId: Int) {
        activity.runOnUiThread { activity.findViewById<IconButton>(viewId).performClick() }
    }

    private fun selected(activity: Activity, viewId: Int): Boolean {
        return activity.findViewById<IconButton>(viewId).isSelected
    }

    private fun exchange(): Place {
        return place(id = 100L, name = "Paris ATM", icon = "local_atm")
    }

    private fun event(): Event {
        return Event(
            id = 200L,
            lat = ParisPlaces.CENTER_LAT,
            lon = ParisPlaces.CENTER_LON,
            name = "Paris Meetup",
            website = null,
            startsAt = ZonedDateTime.parse("2999-01-01T18:00:00Z"),
            endsAt = null,
        )
    }

    private fun place(id: Long, name: String, icon: String): Place {
        return Place(
            id = id,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = ParisPlaces.CENTER_LAT,
            lon = ParisPlaces.CENTER_LON,
            icon = icon,
            name = name,
            localizedName = null,
            verifiedAt = ZonedDateTime.parse("2026-01-01T00:00:00Z"),
            address = null,
            openingHours = null,
            phone = null,
            website = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            requiredAppUrl = null,
            boostedUntil = null,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }
}
