package org.btcmap.platform

/**
 * A reentrant mutual-exclusion lock, the common stand-in for the platform's
 * own lock. The JVM and Android use a `ReentrantLock`; a single-threaded web
 * target needs no real locking.
 */
expect class PlatformLock() {
    fun lock()
    fun unlock()
    fun isHeldByCurrentThread(): Boolean
}
