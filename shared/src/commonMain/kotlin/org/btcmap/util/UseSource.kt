package org.btcmap.util

import okio.Source

/** Runs [block] with this source and closes it afterwards, even on failure. */
inline fun <T : Source, R> T.useSource(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}
