package org.btcmap.util

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
