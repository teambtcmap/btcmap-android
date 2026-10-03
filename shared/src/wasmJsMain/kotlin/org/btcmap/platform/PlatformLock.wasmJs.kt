package org.btcmap.platform

/**
 * A reentrant lock for the single-threaded browser. There is no concurrent
 * access, so a depth counter is enough to keep the reentrancy contract.
 */
actual class PlatformLock actual constructor() {
    private var depth = 0

    actual fun lock() {
        depth++
    }

    actual fun unlock() {
        if (depth > 0) depth--
    }

    actual fun isHeldByCurrentThread(): Boolean = depth > 0
}
