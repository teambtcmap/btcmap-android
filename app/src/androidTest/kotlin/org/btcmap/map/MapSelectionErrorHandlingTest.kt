package org.btcmap.map

import android.graphics.PointF
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.settings.mapViewport
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@RunWith(AndroidJUnit4::class)
class MapSelectionErrorHandlingTest {

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
    fun handleClick_whenLookupFails_doesNotLeakUncaughtException() {
        val driver = FailingDriver()
        val db = Database(driver, ":memory:")
        db.place.insert(ParisPlaces.places)
        app.dbForTesting = db
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var mapView: MapView
                var map: MapLibreMap? = null

                scenario.onActivity { activity ->
                    val mapFragment = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as MapFragment
                    mapView = mapFragment.requireView().findViewById(R.id.map)
                    mapView.getMapAsync { m -> map = m }
                }

                fun markerHitLatLng(): LatLng {
                    val currentMap = map!!
                    val anchor = currentMap.projection.toScreenLocation(
                        LatLng(ParisPlaces.target.lat, ParisPlaces.target.lon)
                    )
                    val inside = PointF(anchor.x, anchor.y - MARKER_HIT_OFFSET_PX)
                    return currentMap.projection.fromScreenLocation(inside)
                }

                waitUntilOnMain {
                    val currentMap = map ?: return@waitUntilOnMain false
                    val anchor = currentMap.projection.toScreenLocation(
                        LatLng(ParisPlaces.target.lat, ParisPlaces.target.lon)
                    )
                    val inside = PointF(anchor.x, anchor.y - MARKER_HIT_OFFSET_PX)
                    currentMap.queryRenderedFeatures(inside, MERCHANT_MARKER_LAYER_ID).isNotEmpty()
                }

                val controller = MapSelectionController(
                    map = map!!,
                    db = db,
                    onOpenPlace = { },
                    onOpenEventWebsite = { },
                    onNoHit = { },
                )
                val handleClick = MapSelectionController::class.java
                    .getDeclaredMethod("handleClick", LatLng::class.java)
                    .apply { isAccessible = true }

                assertNoUncaughtException(
                    "Exception from a marker tap escaped to the uncaught handler",
                ) {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        driver.failing = true
                        handleClick.invoke(controller, markerHitLatLng())
                    }
                }

                Assert.assertTrue(
                    "Marker tap did not reach the database lookup",
                    driver.failedPrepare,
                )
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private class FailingDriver : SQLiteDriver {
        @Volatile
        var failing = false

        @Volatile
        var failedPrepare = false

        private val delegate = AndroidSQLiteDriver()

        override fun open(path: String): SQLiteConnection {
            val connection = delegate.open(path)
            return object : SQLiteConnection {
                override fun prepare(sql: String): SQLiteStatement {
                    if (failing) {
                        failedPrepare = true
                        throw RuntimeException()
                    }
                    return connection.prepare(sql)
                }

                override fun close() {
                    connection.close()
                }
            }
        }
    }

    companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
        private const val MARKER_HIT_OFFSET_PX = 24f
    }
}
