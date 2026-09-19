package org.btcmap.map

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.settings.mapViewport
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class MapAreasCacheTest {

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
    fun areaContainingTheViewport_isShownFromTheCache() {
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.event.insert(listOf(event()))

        preferencesRule.prefs.mapViewport = ParisPlaces.bounds
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var areasList: RecyclerView

                scenario.onActivity { activity ->
                    val mapFragment = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as MapFragment
                    areasList = mapFragment.requireView().findViewById(R.id.areas)
                }

                // The area and its event badge come from the local cache, so no
                // /v4/areas?lat=&lon= request is involved.
                waitUntilOnMain { (areasList.adapter?.itemCount ?: 0) > 0 }

                scenario.onActivity {
                    val row = (areasList.adapter as AreasAdapter).currentList.single()
                    Assert.assertEquals("Grand Paris", row.name)
                    Assert.assertEquals("community", row.type)
                    Assert.assertEquals(1, row.upcomingEventsCount)
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun countries_areListedBeforeCommunities() {
        // Inserted in the wrong order on purpose: the country has a lower id,
        // but row order alone should not decide the chip order.
        databaseRule.db.area.insert(
            listOf(
                area(id = 7L, name = "Grand Paris"),
                area(id = 1L, name = "France", type = "country"),
            ),
        )

        preferencesRule.prefs.mapViewport = ParisPlaces.bounds
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var areasList: RecyclerView

                scenario.onActivity { activity ->
                    val mapFragment = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as MapFragment
                    areasList = mapFragment.requireView().findViewById(R.id.areas)
                }

                waitUntilOnMain {
                    (areasList.adapter?.itemCount ?: 0) >= 2 && areasList.childCount >= 2
                }

                scenario.onActivity {
                    val types = (areasList.adapter as AreasAdapter).currentList.map { it.type }
                    Assert.assertEquals(listOf("country", "community"), types)
                    // The chips render top-down, so the country must also be the
                    // topmost child; a reversed layout manager would flip this.
                    Assert.assertTrue(areasList.getChildAt(0).top < areasList.getChildAt(1).top)
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun area(
        id: Long = 7L,
        name: String = "Grand Paris",
        type: String = "community",
    ): Area {
        return Area(
            id = id,
            name = name,
            type = type,
            urlAlias = "grand-paris",
            icon = null,
            iconWide = null,
            websiteUrl = "https://btcmap.org/community/grand-paris",
            description = null,
            bboxWest = 2.3,
            bboxSouth = 48.85,
            bboxEast = 2.4,
            bboxNorth = 48.87,
            geoJson = """{"type":"Polygon","coordinates":[[[2.3,48.85],[2.4,48.85],[2.4,48.87],[2.3,48.87],[2.3,48.85]]]}""",
        )
    }

    private fun event(): Event {
        return Event(
            id = 1L,
            areaId = 7L,
            lat = 48.86,
            lon = 2.35,
            name = "Meetup",
            website = null,
            startsAt = ZonedDateTime.parse("2030-01-01T00:00:00Z"),
            endsAt = null,
        )
    }

    private companion object {
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
