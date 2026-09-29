package org.btcmap.map

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.btcmap.MainDispatcherRule
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.search.SearchAdapterItem
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.geometry.LatLng
import java.time.ZonedDateTime

/**
 * Exercises [SearchController] on the JVM against an in-memory database.
 *
 * The production constructor depends on `android.location.Location` and
 * `android.content.res.Resources`, which are unusable off-device, so the
 * platform-backed distance and formatting are injected and the cache queries
 * run on the test dispatcher. This keeps the ranking, boost and filtering rules
 * fast and deterministic to cover, unlike the instrumented search tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database = Database(BundledSQLiteDriver(), ":memory:")

    private val reference = LatLng(48.8566, 2.3522)

    private fun controller() = SearchController(
        db = database,
        formatDistance = { meters -> "${meters.toInt()} m" },
        distanceInMeters = { from, to -> coordinateDistance(from, to) },
        ioDispatcher = mainDispatcherRule.dispatcher,
    )

    @Test
    fun search_shorterThanTheMinimum_returnsNoResults() = runTest {
        database.place.insert(listOf(place(id = 1L, name = "Bitcoin Cafe")))
        val controller = controller()

        controller.search(reference, "bi")
        advanceUntilIdle()

        Assert.assertTrue(controller.results.value.isEmpty())
        controller.dispose()
    }

    @Test
    fun search_ranksExactThenPrefixThenSubstring() = runTest {
        // Same location, so only the name rank separates them.
        database.place.insert(
            listOf(
                place(id = 1L, name = "Cafe de Paris"),
                place(id = 2L, name = "Paris Cafe"),
                place(id = 3L, name = "Paris"),
            )
        )
        val controller = controller()

        controller.search(reference, "paris")
        advanceUntilIdle()

        Assert.assertEquals(
            listOf("Paris", "Paris Cafe", "Cafe de Paris"),
            controller.results.value.map { it.name },
        )
        controller.dispose()
    }

    @Test
    fun search_promotesBoostedPlacesAboveRelevance() = runTest {
        database.place.insert(
            listOf(
                place(id = 1L, name = "Paris"),
                place(
                    id = 2L,
                    name = "Paris Cafe",
                    boostedUntil = ZonedDateTime.parse("2999-01-01T00:00:00Z"),
                ),
            )
        )
        val controller = controller()

        controller.search(reference, "paris")
        advanceUntilIdle()

        Assert.assertEquals(
            listOf("Paris Cafe", "Paris"),
            controller.results.value.map { it.name },
        )
        Assert.assertEquals(
            listOf(true, false),
            controller.results.value.map { (it as SearchAdapterItem.Place).boosted },
        )
        controller.dispose()
    }

    @Test
    fun search_capsResultsAtTwenty() = runTest {
        database.place.insert((1L..25L).map { place(id = it, name = "Bitcoin Place $it") })
        val controller = controller()

        controller.search(reference, "bitcoin")
        advanceUntilIdle()

        Assert.assertEquals(20, controller.results.value.size)
        controller.dispose()
    }

    @Test
    fun search_areaWithoutBbox_hasNoDistance() = runTest {
        database.area.insert(listOf(area(id = 1L, name = "Paris Bitcoin", withBbox = false)))
        val controller = controller()

        controller.search(reference, "paris")
        advanceUntilIdle()

        val item = controller.results.value.single() as SearchAdapterItem.Area
        Assert.assertNull(item.bbox)
        Assert.assertNull(item.distanceToUser)
        controller.dispose()
    }

    @Test
    fun search_excludesPastEvents() = runTest {
        database.event.insert(
            listOf(
                event(id = 1L, name = "Bitcoin Past", startsAt = "2020-01-01T00:00:00Z"),
                event(id = 2L, name = "Bitcoin Future", startsAt = "2999-01-01T00:00:00Z"),
            )
        )
        val controller = controller()

        controller.search(reference, "bitcoin")
        advanceUntilIdle()

        Assert.assertEquals(
            listOf("Bitcoin Future"),
            controller.results.value.map { it.name },
        )
        controller.dispose()
    }

    @Test
    fun search_theLatestQueryWins() = runTest {
        database.place.insert(
            listOf(
                place(id = 1L, name = "Bitcoin Cafe"),
                place(id = 2L, name = "Ethereum Cafe"),
            )
        )
        val controller = controller()

        controller.search(reference, "bitcoin")
        controller.search(reference, "ethereum")
        advanceUntilIdle()

        Assert.assertEquals(
            listOf("Ethereum Cafe"),
            controller.results.value.map { it.name },
        )
        controller.dispose()
    }

    /**
     * A monotonic stand-in for the platform geodesic distance: enough to order
     * results, without `android.location.Location`.
     */
    private fun coordinateDistance(from: LatLng, to: LatLng): Double {
        val dLat = from.latitude - to.latitude
        val dLon = from.longitude - to.longitude
        return Math.sqrt(dLat * dLat + dLon * dLon)
    }

    private fun place(
        id: Long,
        name: String?,
        lat: Double = reference.latitude,
        lon: Double = reference.longitude,
        boostedUntil: ZonedDateTime? = null,
    ): Place {
        return Place(
            id = id,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = lat,
            lon = lon,
            icon = "local_cafe",
            name = name,
            localizedName = null,
            verifiedAt = null,
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
            boostedUntil = boostedUntil,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }

    private fun area(
        id: Long,
        name: String,
        withBbox: Boolean = true,
    ): Area {
        return Area(
            id = id,
            name = name,
            type = "community",
            urlAlias = "paris",
            icon = null,
            iconWide = null,
            websiteUrl = "https://btcmap.org/community/paris",
            description = null,
            bboxWest = if (withBbox) 2.22 else null,
            bboxSouth = if (withBbox) 48.81 else null,
            bboxEast = if (withBbox) 2.47 else null,
            bboxNorth = if (withBbox) 48.91 else null,
            geoJson = null,
        )
    }

    private fun event(
        id: Long,
        name: String,
        startsAt: String,
    ): Event {
        return Event(
            id = id,
            lat = reference.latitude,
            lon = reference.longitude,
            name = name,
            website = null,
            startsAt = ZonedDateTime.parse(startsAt),
            endsAt = null,
        )
    }
}
