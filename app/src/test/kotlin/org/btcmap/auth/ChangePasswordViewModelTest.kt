package org.btcmap.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.btcmap.api.Api
import org.btcmap.api.ApiException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Exercises the change-password state machine against a real HTTP server. Like
 * [AuthViewModelTest], the view model's scope runs on [Dispatchers.Default]
 * rather than a virtual test dispatcher because the request really blocks on
 * OkHttp.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private val server: MockWebServer
        get() = serverRule.server

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun api(): Api = Api(
        httpClient = OkHttpClient(),
        baseUrl = { server.url("/") },
    )

    private fun viewModel(): ChangePasswordViewModel = ChangePasswordViewModel(api())

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
        )
    }

    @Test
    fun change_success_emitsChanged() = runTest {
        enqueueJson("{}")

        val model = viewModel()
        model.change(currentPassword = "old", newPassword = "new")

        Assert.assertTrue(model.events.first() is ChangePasswordEvent.Changed)
    }

    @Test
    fun change_failure_emitsFailed() = runTest {
        enqueueJson("""{"message":"Invalid old password"}""", code = 400)

        val model = viewModel()
        model.change(currentPassword = "wrong", newPassword = "new")

        val event = model.events.first()
        Assert.assertTrue(event is ChangePasswordEvent.Failed)
        val error = (event as ChangePasswordEvent.Failed).error
        Assert.assertTrue(error is ApiException)
        Assert.assertEquals(400, (error as ApiException).code)
    }

    @Test
    fun change_whileRequestInFlight_isIgnored() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("{}")
                .bodyDelay(200, TimeUnit.MILLISECONDS)
                .build()
        )

        val model = viewModel()
        model.change(currentPassword = "old", newPassword = "new")
        model.change(currentPassword = "old", newPassword = "new")

        model.events.first()

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun busy_isRaisedWhileInFlightAndClearedAfter() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("{}")
                .bodyDelay(200, TimeUnit.MILLISECONDS)
                .build()
        )

        val model = viewModel()
        model.change(currentPassword = "old", newPassword = "new")

        model.busy.first { it }
        Assert.assertTrue(model.busy.value)

        model.events.first()
        model.busy.first { !it }
        Assert.assertFalse(model.busy.value)
    }

    @Test
    fun cancel_stopsTheRequestAndClearsBusy() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("{}")
                .bodyDelay(5, TimeUnit.SECONDS)
                .build()
        )

        val model = viewModel()
        model.change(currentPassword = "old", newPassword = "new")
        model.busy.first { it }

        model.cancel()
        model.busy.first { !it }

        Assert.assertFalse(model.busy.value)
    }

    @Test
    fun factory_createsAChangePasswordViewModel() {
        val model = ChangePasswordViewModel.Factory(api())
            .create(ChangePasswordViewModel::class.java)

        Assert.assertNotNull(model)
    }

    @Test
    fun factory_rejectsAnUnrelatedViewModelClass() {
        val factory = ChangePasswordViewModel.Factory(api())

        Assert.assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnrelatedChangePasswordViewModel::class.java)
        }
    }
}

private class UnrelatedChangePasswordViewModel : ViewModel()
