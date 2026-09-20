package org.btcmap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.btcmap.util.rethrowIfCancellation
import java.time.Duration

/**
 * Owns the full sync: it seeds each table from its bundled snapshot and then
 * pulls the deltas from the API.
 *
 * The manager is app-scoped, so a sync started from the map keeps running after
 * the map screen is closed, and screens observe [state] and [events] instead of
 * driving the work themselves. Every step takes [mutex], because the bundled
 * imports and the delta syncs share a single database connection and a screen
 * (the comments screen) can ask for a comments sync while the full sync is
 * still running.
 */
internal class SyncManager(
    private val sync: () -> Sync,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val seedPlaces: suspend () -> Long,
    private val seedEvents: suspend () -> Long,
    private val seedComments: suspend () -> Long,
    private val seedAreas: suspend () -> Long,
) : SyncController {
    private val mutex = Mutex()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>(
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private var job: Job? = null

    /** Starts a full sync unless one is already running. */
    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runFullSync() }
    }

    /**
     * Syncs only the comments, for a screen that has to wait for its own change
     * rather than for the whole full sync, e.g. the post-payment retry on the
     * comments screen.
     */
    override suspend fun syncComments(): Sync.CommentSyncReport = mutex.withLock {
        try {
            val report = step(SyncState.SyncingComments) { sync().syncComments() }
                ?: Sync.CommentSyncReport(duration = Duration.ZERO, rowsAffected = 0L, failed = true)

            if (report.rowsAffected > 0) {
                emit(SyncEvent.CommentsChanged)
            }

            report
        } finally {
            _state.value = SyncState.Idle
        }
    }

    internal suspend fun runFullSync() = mutex.withLock {
        try {
            val placesImported = step(SyncState.UnbundlingPlaces) { seedPlaces() } ?: 0L
            val places = step(SyncState.SyncingPlaces) { sync().syncPlaces() }
            if (placesImported > 0 || (places?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.PlacesChanged)
            }

            val eventsImported = step(SyncState.UnbundlingEvents) { seedEvents() } ?: 0L
            val events = step(SyncState.SyncingEvents) { sync().syncEvents() }
            if (eventsImported > 0 || (events?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.EventsChanged)
            }

            // Comments are seeded too, so a place's comments work offline; only
            // a sync that changed something has to rebuild the map.
            step(SyncState.UnbundlingComments) { seedComments() }
            val comments = step(SyncState.SyncingComments) { sync().syncComments() }
            if ((comments?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.CommentsChanged)
            }

            val areasImported = step(SyncState.UnbundlingAreas) { seedAreas() } ?: 0L
            val areas = step(SyncState.SyncingAreas) { sync().syncAreas() }
            if (areasImported > 0 || (areas?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.AreasChanged)
            }
        } finally {
            _state.value = SyncState.Idle
        }
    }

    /**
     * Runs one sync step and returns its result, or null when it failed.
     *
     * The sync is app-scoped, so a failure must not escape: an unhandled
     * exception on the manager's own scope would crash the process, and a
     * database that is temporarily unusable must not stop the remaining steps.
     * [Sync] already guards its network and apply steps; this also covers the
     * cursor read before them and any other unexpected failure.
     */
    private suspend fun <T> step(state: SyncState, block: suspend () -> T): T? {
        _state.value = state
        return try {
            block()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            t.printStackTrace()
            null
        }
    }

    private fun emit(event: SyncEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val EVENT_BUFFER = 16
    }
}
