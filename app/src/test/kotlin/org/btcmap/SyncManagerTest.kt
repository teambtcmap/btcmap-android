package org.btcmap

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun createDatabase() = Database(BundledSQLiteDriver(), ":memory:")

    private fun createApi() = Api(
        httpClient = OkHttpClient(),
        baseUrl = { serverRule.server.url("/") },
    )

    private fun enqueueEmpty() {
        repeat(4) {
            serverRule.server.enqueue(
                MockResponse.Builder()
                    .addHeader("Content-Type", "application/json")
                    .body("[]")
                    .build(),
            )
        }
    }

    private fun manager(
        sync: () -> Sync,
        seedPlaces: suspend () -> Long = { 0L },
        seedEvents: suspend () -> Long = { 0L },
        seedComments: suspend () -> Long = { 0L },
        seedAreas: suspend () -> Long = { 0L },
    ) = SyncManager(
        sync = sync,
        seedPlaces = seedPlaces,
        seedEvents = seedEvents,
        seedComments = seedComments,
        seedAreas = seedAreas,
    )

    @Test
    fun runFullSync_syncsEachTableInOrderAndEmitsChanges() = runTest {
        enqueueEmpty()
        val sync = Sync(createApi(), createDatabase())

        val subject = manager(
            sync = { sync },
            seedPlaces = { 1L },
            seedEvents = { 1L },
            seedComments = { 0L },
            seedAreas = { 1L },
        )

        val events = mutableListOf<SyncEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            subject.events.collect { events += it }
        }

        subject.runFullSync()

        Assert.assertEquals(SyncState.Idle, subject.state.value)
        Assert.assertEquals(
            listOf(SyncEvent.PlacesChanged, SyncEvent.EventsChanged, SyncEvent.AreasChanged),
            events,
        )

        val paths = (1..4).map { serverRule.server.takeRequest().url.encodedPath }
        Assert.assertEquals(
            listOf("/v4/places", "/v4/events", "/v4/place-comments", "/v4/areas"),
            paths,
        )
    }

    @Test
    fun runFullSync_publishesEachUnbundlingPhaseBeforeSeeding() = runTest {
        enqueueEmpty()
        val sync = Sync(createApi(), createDatabase())

        lateinit var subject: SyncManager
        val phases = mutableListOf<SyncState>()
        subject = manager(
            sync = { sync },
            seedPlaces = { phases += subject.state.value; 0L },
            seedEvents = { phases += subject.state.value; 0L },
            seedComments = { phases += subject.state.value; 0L },
            seedAreas = { phases += subject.state.value; 0L },
        )

        subject.runFullSync()

        Assert.assertEquals(
            listOf(
                SyncState.UnbundlingPlaces,
                SyncState.UnbundlingEvents,
                SyncState.UnbundlingComments,
                SyncState.UnbundlingAreas,
            ),
            phases,
        )
        Assert.assertEquals(SyncState.Idle, subject.state.value)
    }

    @Test
    fun runFullSync_keepsGoingWhenAStepFails() = runTest {
        enqueueEmpty()
        val sync = Sync(createApi(), createDatabase())

        val seeded = mutableListOf<String>()
        val subject = manager(
            sync = { sync },
            seedPlaces = { seeded += "places"; 1L },
            seedEvents = { throw RuntimeException("event seed failed") },
            seedComments = { seeded += "comments"; 0L },
            seedAreas = { seeded += "areas"; 0L },
        )

        // Must not throw: the app-scoped sync catches a failed step so it can
        // neither crash the process nor stop the remaining tables.
        subject.runFullSync()

        Assert.assertEquals(listOf("places", "comments", "areas"), seeded)
        Assert.assertEquals(SyncState.Idle, subject.state.value)
    }

    @Test
    fun syncComments_reportsFailureInsteadOfThrowing() = runTest {
        val subject = manager(sync = { throw RuntimeException("database unavailable") })

        val report = subject.syncComments()

        Assert.assertTrue(report.failed)
        Assert.assertEquals(0L, report.rowsAffected)
        Assert.assertEquals(SyncState.Idle, subject.state.value)
    }

    @Test
    fun start_doesNotRunTwoFullSyncsAtOnce() = runTest {
        val seeds = AtomicInteger()
        val release = CompletableDeferred<Unit>()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        try {
            val subject = SyncManager(
                sync = { throw RuntimeException("not reached in this test") },
                scope = scope,
                seedPlaces = { seeds.incrementAndGet(); release.await(); 0L },
                seedEvents = { 0L },
                seedComments = { 0L },
                seedAreas = { 0L },
            )

            // The first run starts eagerly and parks in seedPlaces; the second
            // start must see it and not launch another full sync.
            subject.start()
            subject.start()
            Assert.assertEquals(1, seeds.get())

            release.complete(Unit)
            Assert.assertEquals(SyncState.Idle, subject.state.value)
        } finally {
            scope.cancel()
        }
    }
}
