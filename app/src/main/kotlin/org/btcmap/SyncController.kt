package org.btcmap

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The observable face of the app-scoped sync.
 *
 * Screens depend on this rather than on [SyncManager] so a test can install a
 * controller that keeps the background full sync out of the way while leaving
 * an explicitly requested comments sync working.
 */
internal interface SyncController {
    val state: StateFlow<SyncState>
    val events: SharedFlow<SyncEvent>

    /** Starts a full sync unless one is already running. */
    fun start()

    /**
     * Syncs only the comments, for a screen that has to wait for its own change
     * rather than for the whole full sync.
     */
    suspend fun syncComments(): Sync.CommentSyncReport
}
