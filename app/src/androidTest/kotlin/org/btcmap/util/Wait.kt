package org.btcmap.util

import androidx.test.platform.app.InstrumentationRegistry

fun waitUntil(
    timeoutMs: Long = 15_000,
    intervalMs: Long = 100,
    condition: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        Thread.sleep(intervalMs)
    }
    throw AssertionError("Condition was not met within ${timeoutMs}ms")
}

fun waitUntilOnMain(
    timeoutMs: Long = 15_000,
    intervalMs: Long = 100,
    condition: () -> Boolean,
) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    waitUntil(timeoutMs = timeoutMs, intervalMs = intervalMs) {
        var result = false
        instrumentation.runOnMainSync { result = condition() }
        result
    }
}
