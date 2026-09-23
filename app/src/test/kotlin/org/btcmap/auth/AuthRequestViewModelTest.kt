package org.btcmap.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.btcmap.MainDispatcherRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * Exercises the shared request machinery in isolation. A [CompletableDeferred]
 * gate stands in for the network call, so the request lifecycle and the
 * cancellation race can be driven deterministically instead of through a real
 * HTTP server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRequestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun completingRequest_clearsBusy() = runTest(mainDispatcherRule.dispatcher) {
        val model = GatedRequestViewModel()
        val gate = CompletableDeferred<Unit>()

        model.run(gate)
        runCurrent()
        Assert.assertTrue(model.busy.value)

        gate.complete(Unit)
        advanceUntilIdle()
        Assert.assertFalse(model.busy.value)
    }

    @Test
    fun cancelledRequest_clearsBusyWhenNothingReplacedIt() =
        runTest(mainDispatcherRule.dispatcher) {
            val model = GatedRequestViewModel()
            val gate = CompletableDeferred<Unit>()

            model.run(gate)
            runCurrent()
            Assert.assertTrue(model.busy.value)

            model.cancel()
            advanceUntilIdle()

            Assert.assertFalse(model.busy.value)
        }

    @Test
    fun cancelledRequest_doesNotClearTheBusyFlagOfTheRequestThatReplacedIt() =
        runTest(mainDispatcherRule.dispatcher) {
            val model = GatedRequestViewModel()
            val first = CompletableDeferred<Unit>()
            val second = CompletableDeferred<Unit>()

            model.run(first)
            runCurrent()
            Assert.assertTrue(model.busy.value)

            // Cancel the first request and immediately start a replacement. The
            // first coroutine has not unwound yet, so its `finally` runs only
            // after the replacement has already raised the flag; it must not
            // clear the flag of the request that replaced it.
            model.cancel()
            model.run(second)
            runCurrent()

            Assert.assertTrue(model.busy.value)

            second.complete(Unit)
            advanceUntilIdle()
            Assert.assertFalse(model.busy.value)
        }

    private class GatedRequestViewModel : AuthRequestViewModel<String>() {
        fun run(gate: CompletableDeferred<Unit>) = launchRequest { gate.await() }
    }
}
