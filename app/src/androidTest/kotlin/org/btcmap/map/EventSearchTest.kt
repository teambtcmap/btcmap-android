package org.btcmap.map

import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.search.SearchView
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.event.EventFragment
import org.btcmap.search.SearchAdapter
import org.btcmap.search.SearchAdapterItem
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class EventSearchTest {

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

    private val reference = LatLng(48.8566, 2.3522)

    @Test
    fun offlineSearch_returnsMatchingEvent() {
        databaseRule.db.event.insert(
            listOf(
                event(
                    id = 1L,
                    name = "Bitcoin Meetup Paris",
                    lat = 48.8570,
                    lon = 2.3530,
                )
            )
        )
        val controller = searchController()
        try {
            controller.search(reference, "bitcoin")

            waitUntil { controller.results.value.isNotEmpty() }

            val item = controller.results.value.single()
            Assert.assertTrue(item is SearchAdapterItem.Event)
            item as SearchAdapterItem.Event
            Assert.assertEquals(1L, item.eventId)
            Assert.assertEquals("Bitcoin Meetup Paris", item.name)
            Assert.assertEquals(EVENT_ICON, item.icon)
            Assert.assertNotNull(item.distanceToUser)
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun offlineSearch_sortsPlacesAndEventsByDistance() {
        // The event is next to the reference point, the place is far away.
        databaseRule.db.place.insert(
            listOf(
                place(
                    id = 1L,
                    name = "Bitcoin Cafe Far",
                    lat = 48.90,
                    lon = 2.3522,
                )
            )
        )
        databaseRule.db.event.insert(
            listOf(
                event(
                    id = 2L,
                    name = "Bitcoin Meetup Near",
                    lat = 48.8567,
                    lon = 2.3522,
                )
            )
        )
        val controller = searchController()
        try {
            controller.search(reference, "bitcoin")

            waitUntil { controller.results.value.size == 2 }

            Assert.assertEquals(
                listOf("Bitcoin Meetup Near", "Bitcoin Cafe Far"),
                controller.results.value.map { it.name },
            )
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun offlineSearch_doesNotMatchEventWebsite() {
        // The place matches on name and is the signal that the search finished;
        // the event only matches on its website, so it must be absent.
        databaseRule.db.place.insert(
            listOf(
                place(
                    id = 1L,
                    name = "Bitcoin Cafe",
                    lat = 48.8566,
                    lon = 2.3522,
                )
            )
        )
        databaseRule.db.event.insert(
            listOf(
                event(
                    id = 2L,
                    name = "London BTC",
                    lat = 51.5074,
                    lon = -0.1278,
                    website = "https://bitcoin.example.com",
                )
            )
        )
        val controller = searchController()
        try {
            controller.search(reference, "bitcoin")

            waitUntil { controller.results.value.any { it is SearchAdapterItem.Place } }

            Assert.assertTrue(
                controller.results.value.none { it is SearchAdapterItem.Event },
            )
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun onlineSearch_returnsEventFromServer() {
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body(
                        """
                        {"results":[{"type":"event","id":42,"name":"Bitcoin Meetup","lat":48.8566,"lon":2.3522}]}
                        """.trimIndent()
                    )
                    .build()
            }
        }
        val controller = searchController(isOnline = true)
        try {
            controller.search(reference, "bitcoin")

            waitUntil { controller.results.value.any { it is SearchAdapterItem.Event } }

            val item = controller.results.value
                .filterIsInstance<SearchAdapterItem.Event>()
                .single()
            Assert.assertEquals(42L, item.eventId)
            Assert.assertEquals("Bitcoin Meetup", item.name)
            Assert.assertEquals(EVENT_ICON, item.icon)
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun tappingEventResult_opensEventFragment() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            // Fail the event sync so it cannot wipe the row the tap needs; the
            // search result is injected directly, so the server is irrelevant.
            apiRule.server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return if (request.url.encodedPath.contains("/events")) {
                        MockResponse.Builder().code(404).build()
                    } else {
                        MockResponse.Builder()
                            .code(200)
                            .addHeader("Content-Type", "application/json")
                            .body("[]")
                            .build()
                    }
                }
            }

            val stored = event(
                id = 7L,
                name = "Bitcoin Meetup Paris",
                lat = 48.8566,
                lon = 2.3522,
            )
            databaseRule.db.event.insert(listOf(stored))

            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var mapFragment: MapFragment
                lateinit var results: RecyclerView
                lateinit var adapter: SearchAdapter

                scenario.onActivity {
                    mapFragment = it.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as MapFragment
                    results = mapFragment.requireView().findViewById(R.id.searchResults)
                    adapter = results.adapter as SearchAdapter
                    mapFragment.requireView()
                        .findViewById<SearchView>(R.id.searchView)
                        .show()
                    adapter.submitList(
                        listOf(
                            SearchAdapterItem.Event(
                                eventId = stored.id,
                                icon = EVENT_ICON,
                                name = stored.name,
                                distanceToUser = null,
                            )
                        )
                    )
                }

                waitUntilOnMain { results.childCount == 1 }
                scenario.onActivity { results.getChildAt(0).performClick() }

                waitUntilOnMain {
                    mapFragment.parentFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) is EventFragment
                }

                scenario.onActivity {
                    val eventFragment = mapFragment.parentFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as EventFragment
                    val title = eventFragment.requireView()
                        .findViewById<Toolbar>(R.id.toolbar).title
                    Assert.assertEquals(stored.name, title)
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun searchController(isOnline: Boolean = false): SearchController {
        return SearchController(
            db = databaseRule.db,
            api = apiRule.api,
            resources = app.resources,
            isOnline = { isOnline },
        )
    }

    private fun place(id: Long, name: String, lat: Double, lon: Double): Place {
        return Place(
            id = id,
            bundled = false,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = lat,
            lon = lon,
            icon = "local_cafe",
            name = name,
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            localizedOpeningHours = null,
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

    private fun event(
        id: Long,
        name: String,
        lat: Double,
        lon: Double,
        website: String? = null,
    ): Event {
        return Event(
            id = id,
            areaId = null,
            lat = lat,
            lon = lon,
            name = name,
            website = website?.toHttpUrl(),
            startsAt = ZonedDateTime.parse("2999-01-01T18:00:00Z"),
            endsAt = null,
        )
    }

    companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
