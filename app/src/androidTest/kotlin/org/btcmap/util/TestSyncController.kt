package org.btcmap.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.btcmap.sync.Sync
import org.btcmap.sync.SyncController
import org.btcmap.sync.SyncEvent
import org.btcmap.sync.SyncRunStats
import org.btcmap.sync.SyncState

/**
 * A [SyncController] for tests that do not exercise the app-scoped sync.
 *
 * The background full sync is disabled: it never seeds the real bundled
 * snapshots or talks to the API, so a test that only needs mocked network calls
 * or rows in the local database is left undisturbed. The comments sync still
 * runs, because screens such as the comments list drive it explicitly and tests
 * rely on that.
 */
internal class TestSyncController(
    private val sync: () -> Sync,
) : SyncController {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val state: StateFlow<SyncState> = mutableState

    private val mutableEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<SyncEvent> = mutableEvents

    private val mutableLastSyncStats = MutableStateFlow<SyncRunStats?>(null)
    override val lastSyncStats: StateFlow<SyncRunStats?> = mutableLastSyncStats

    /**
     * Publishes a change the way a finished sync step would, so a test can drive
     * a screen that refreshes itself when the app-scoped sync touches its data.
     */
    fun emit(event: SyncEvent) {
        mutableEvents.tryEmit(event)
    }

    /** How many times [start] was called, for tests that trigger a sync. */
    @Volatile
    var startedCount: Int = 0
        private set

    override fun start() {
        startedCount++
    }

    /**
     * Moves the observed state, so a test can drive a screen that reacts to the
     * sync starting and finishing (e.g. the database stats refresh).
     */
    fun setState(state: SyncState) {
        mutableState.value = state
    }

    override suspend fun syncComments(): Sync.Report = sync().syncComments()
}
