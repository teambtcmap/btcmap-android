package org.btcmap.ui

import kotlinx.coroutines.withContext
import org.btcmap.platform.ioDispatcher
import org.btcmap.util.rethrowIfCancellation

/**
 * Reads a secondary section on the IO dispatcher, hiding it when the read or
 * fetch fails. The callers run on the UI dispatcher; the section's reads must
 * not.
 */
suspend fun <T> loadSection(block: suspend () -> T): T? = try {
    withContext(ioDispatcher) { block() }
} catch (t: Throwable) {
    t.rethrowIfCancellation()
    null
}
