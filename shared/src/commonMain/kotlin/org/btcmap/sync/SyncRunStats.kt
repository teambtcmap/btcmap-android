package org.btcmap.sync

import kotlin.time.Duration

/**
 * How long one step of a full sync run took, keyed by the [SyncState] the step
 * published while it ran.
 */
data class SyncStepTiming(
    val state: SyncState,
    val duration: Duration,
)

/**
 * The timings of the last completed full sync run, in execution order, with the
 * wall-clock time the whole run took.
 *
 * Kept in memory only: it is a diagnostic for the database stats screen, not
 * persisted state, so it resets when the process restarts.
 */
data class SyncRunStats(
    val steps: List<SyncStepTiming>,
    val total: Duration,
)
