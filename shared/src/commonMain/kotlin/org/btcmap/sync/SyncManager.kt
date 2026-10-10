package org.btcmap.sync

import org.btcmap.platform.ioDispatcher
import kotlinx.coroutines.CoroutineScope
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
import kotlin.time.Clock
import kotlin.time.Duration

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
class SyncManager(
    private val sync: () -> Sync,
    /**
     * Re-reads the signed-in profile from the server and returns true when the
     * cached user changed. Run as the last step of a full sync, so a role
     * granted or revoked elsewhere reaches the role-gated UI without a
     * re-login. The host decides whether a session exists; a signed-out user
     * returns false without a request.
     */
    private val refreshUser: suspend () -> Boolean,
    /**
     * Rewrites the cached personal notes from the server and returns whether they
     * changed. Run with [refreshUser] at the end of a full sync; the host decides
     * whether a session exists, and a signed-out call returns false.
     */
    private val syncNotes: suspend () -> Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
    /**
     * Seeds the places table, invoking the callback with the running total
     * after each committed batch. The import is the slowest part of a fresh
     * install, so its progress is surfaced as [SyncEvent.PlacesChanged] to let
     * the map draw the places that have landed.
     */
    private val seedPlaces: suspend (onBatch: (Long) -> Unit) -> Long,
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

    private val _lastSyncStats = MutableStateFlow<SyncRunStats?>(null)
    override val lastSyncStats: StateFlow<SyncRunStats?> = _lastSyncStats.asStateFlow()

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
    override suspend fun syncComments(): Sync.Report = mutex.withLock {
        try {
            val report = step(SyncState.SyncingComments) { sync().syncComments() }
                ?: Sync.Report(duration = Duration.ZERO, rowsAffected = 0L, failed = true)

            if (report.rowsAffected > 0) {
                emit(SyncEvent.CommentsChanged)
            }

            report
        } finally {
            _state.value = SyncState.Idle
        }
    }

    internal suspend fun runFullSync() = mutex.withLock {
        val runStartedAt = Clock.System.now()
        val timings = mutableListOf<SyncStepTiming>()

        // Times each step as it runs and keeps its duration, so the last run's
        // per-step timings can be published when the run finishes.
        suspend fun <T> timed(state: SyncState, block: suspend () -> T): T? {
            val startedAt = Clock.System.now()
            val result = step(state, block)
            timings.add(SyncStepTiming(state, Clock.System.now() - startedAt))
            return result
        }

        try {
            // Each seeded batch is announced as it commits, so the map fills in
            // while the rest of the snapshot is still being imported rather than
            // staying empty for the whole seed.
            timed(SyncState.UnbundlingPlaces) { seedPlaces { emit(SyncEvent.PlacesChanged) } }
            val places = timed(SyncState.SyncingPlaces) { sync().syncPlaces() }
            if ((places?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.PlacesChanged)
            }

            val eventsImported = timed(SyncState.UnbundlingEvents) { seedEvents() } ?: 0L
            val events = timed(SyncState.SyncingEvents) { sync().syncEvents() }
            if (eventsImported > 0 || (events?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.EventsChanged)
            }

            // Comments are seeded too, so a place's comments work offline; only
            // a sync that changed something has to rebuild the map.
            timed(SyncState.UnbundlingComments) { seedComments() }
            val comments = timed(SyncState.SyncingComments) { sync().syncComments() }
            if ((comments?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.CommentsChanged)
            }

            val areasImported = timed(SyncState.UnbundlingAreas) { seedAreas() } ?: 0L
            val areas = timed(SyncState.SyncingAreas) { sync().syncAreas() }
            if (areasImported > 0 || (areas?.rowsAffected ?: 0L) > 0) {
                emit(SyncEvent.AreasChanged)
            }

            // Refresh the user-scoped state last, so it neither delays the
            // bundled places import (and the first pins) nor the data deltas:
            // the cached personal notes are rewritten, then the profile is
            // re-read and a change is announced so the role-gated UI re-derives.
            if (guarded { syncNotes() } == true) {
                emit(SyncEvent.NotesChanged)
            }
            if (guarded { refreshUser() } == true) {
                emit(SyncEvent.UserChanged)
            }
        } finally {
            _lastSyncStats.value = SyncRunStats(
                steps = timings.toList(),
                total = Clock.System.now() - runStartedAt,
            )
            _state.value = SyncState.Idle
        }
    }

    /**
     * Runs one sync step and returns its result, or null when it failed.
     *
     * The sync is app-scoped, so a recoverable failure must not escape: an
     * unhandled exception on the manager's own scope would crash the process,
     * and a database that is temporarily unusable must not stop the remaining
     * steps. [Sync] already guards its network and apply steps; this also covers
     * the cursor read before them and any other unexpected failure.
     *
     * Only [Exception] is caught, never [Error]: the bundled seeds intentionally
     * let an [Error] propagate so a non-recoverable condition is not mistaken for
     * a successful empty step.
     */
    private suspend fun <T> step(state: SyncState, block: suspend () -> T): T? {
        _state.value = state
        return guarded(block)
    }

    /**
     * [step]'s failure guard without publishing a [SyncState]: used by the
     * profile refresh, which is not a table unbundle or delta and must not add a
     * sync state of its own.
     */
    private suspend fun <T> guarded(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            reportSyncFailure(e)
            null
        }

    private fun emit(event: SyncEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val EVENT_BUFFER = 16
    }
}
