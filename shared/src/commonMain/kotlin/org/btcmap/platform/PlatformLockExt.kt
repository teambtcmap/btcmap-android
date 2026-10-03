package org.btcmap.platform

/** Runs [block] while holding this lock, releasing it even when it throws. */
inline fun <T> PlatformLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
