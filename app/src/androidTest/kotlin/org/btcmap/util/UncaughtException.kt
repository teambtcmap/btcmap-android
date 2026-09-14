package org.btcmap.util

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import java.util.concurrent.atomic.AtomicReference

fun assertNoUncaughtException(
    message: String = "Exception escaped to the uncaught handler",
    action: () -> Unit,
) {
    val uncaught = AtomicReference<Throwable?>()
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        uncaught.set(throwable)
    }
    try {
        action()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline && uncaught.get() == null) {
            instrumentation.waitForIdleSync()
            Thread.sleep(50)
        }
    } finally {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    Assert.assertNull(message, uncaught.get())
}
