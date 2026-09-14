package org.btcmap.map

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.settings.mapViewport
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MapAreasErrorHandlingTest {

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
    fun failedAreasRefresh_keepsTheExistingList() {
        val failing = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val isAreas = request.url.encodedPath == "/v4/areas"
                val fails = isAreas && failing.get()
                return MockResponse.Builder()
                    .code(if (fails) 500 else 200)
                    .addHeader("Content-Type", "application/json")
                    .body(
                        when {
                            fails -> """{"message":"boom"}"""
                            isAreas -> AREAS_JSON
                            else -> "[]"
                        }
                    )
                    .build()
            }
        }

        preferencesRule.prefs.mapViewport = ParisPlaces.bounds
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var areasList: RecyclerView
                var map: MapLibreMap? = null

                scenario.onActivity { activity ->
                    val mapFragment = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as MapFragment
                    areasList = mapFragment.requireView().findViewById(R.id.areas)
                    mapFragment.requireView().findViewById<MapView>(R.id.map)
                        .getMapAsync { m -> map = m }
                }

                waitUntilOnMain { (areasList.adapter?.itemCount ?: 0) > 0 }

                failing.set(true)
                val requestsBefore = apiRule.server.requestCount
                scenario.onActivity {
                    map!!.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(ParisPlaces.target.lat + 0.01, ParisPlaces.target.lon)
                        ),
                        200,
                    )
                }
                waitUntil { apiRule.server.requestCount > requestsBefore }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                scenario.onActivity {
                    Assert.assertTrue(
                        "Areas list should be kept after a failed refresh",
                        (areasList.adapter?.itemCount ?: 0) > 0,
                    )
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

        private const val AREAS_JSON = """
            [
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "upcoming_events": []
                }
            ]
        """
    }
}
