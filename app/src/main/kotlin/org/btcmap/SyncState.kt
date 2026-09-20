package org.btcmap

/**
 * What the app-scoped sync is currently doing.
 *
 * The sync runs for as long as the process lives, so screens observe this state
 * instead of driving the work: the map shows its spinner while the state is
 * anything other than [Idle] and reacts to the [SyncEvent]s a finished step
 * emits. Each entity is seeded from its bundled snapshot (the `Unbundling`
 * states) before its delta is pulled from the API (the `Syncing` states).
 */
internal sealed interface SyncState {
    /** No sync is running. */
    data object Idle : SyncState

    data object UnbundlingPlaces : SyncState
    data object SyncingPlaces : SyncState
    data object UnbundlingEvents : SyncState
    data object SyncingEvents : SyncState
    data object UnbundlingComments : SyncState
    data object SyncingComments : SyncState
    data object UnbundlingAreas : SyncState
    data object SyncingAreas : SyncState
}
