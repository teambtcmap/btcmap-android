package org.btcmap.auth

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Exercises the auth request state machine against a real HTTP server and an
 * in-memory database.
 *
 * The view model's scope runs on [Dispatchers.Default] rather than a virtual
 * test dispatcher: the requests really block on OkHttp, and `withTimeout` on a
 * [kotlinx.coroutines.test.TestDispatcher] would be advanced by `advanceUntilIdle`
 * straight to the 30s deadline before the response could arrive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private val server: MockWebServer
        get() = serverRule.server

    private lateinit var db: Database
    private lateinit var settings: Settings

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        db = Database(BundledSQLiteDriver(), ":memory:")
        settings = Settings(dbProvider = { db }, legacyValues = { emptyMap() })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun api(): Api = Api(
        httpClient = OkHttpClient(),
        baseUrl = { server.url("/") },
    )

    private fun viewModel(): AuthViewModel = AuthViewModel(api(), db, settings)

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
        )
    }

    private fun tokenResponse(token: String, name: String): String =
        """{"token":"$token","user":{"id":1,"name":"$name","roles":["user"]}}"""

    @Test
    fun signIn_success_storesSessionAndEmitsAuthenticated() = runTest {
        enqueueJson(tokenResponse("token-1", "satoshi"))

        val model = viewModel()
        model.signIn("satoshi", "SuperSecure", Bundle())

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.Authenticated)
        Assert.assertEquals("satoshi", (event as AuthEvent.Authenticated).name)
        Assert.assertEquals("token-1", settings.authToken)
        Assert.assertEquals("satoshi", db.user.select()?.name)
    }

    @Test
    fun signIn_failure_emitsFailedAndStaysSignedOut() = runTest {
        enqueueJson("""{"message":"Invalid credentials"}""", code = 401)

        val model = viewModel()
        model.signIn("satoshi", "wrong", Bundle())

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.Failed)
        val failed = event as AuthEvent.Failed
        Assert.assertEquals(AuthOperation.SignIn, failed.operation)
        Assert.assertTrue(failed.error is ApiException)
        Assert.assertEquals(401, (failed.error as ApiException).code)
        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun signUp_success_signsInAndStoresSession() = runTest {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson(tokenResponse("token-1", "Satoshi"))

        val model = viewModel()
        model.signUp("Satoshi", "SuperSecure", Bundle())

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.Authenticated)
        Assert.assertEquals("token-1", settings.authToken)
    }

    @Test
    fun signUp_autoSignInFailure_reportsAccountCreated() = runTest {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson("""{"message":"boom"}""", code = 500)

        val model = viewModel()
        model.signUp("Satoshi", "SuperSecure", Bundle())

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.AccountCreated)
        Assert.assertEquals("Satoshi", (event as AuthEvent.AccountCreated).username)
        Assert.assertNull(settings.authToken)
    }

    @Test
    fun signUp_cancelDuringAutoSignIn_stillReportsAccountCreated() = runTest {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(tokenResponse("token-1", "Satoshi"))
                .bodyDelay(5, TimeUnit.SECONDS)
                .build()
        )

        val model = viewModel()
        model.signUp("Satoshi", "SuperSecure", Bundle())

        // Wait until the account was created and the follow-up sign-in is in
        // flight, then cancel mid-request: the account must still be reported.
        server.takeRequest()
        server.takeRequest()
        model.cancel()

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.AccountCreated)
        Assert.assertEquals("Satoshi", (event as AuthEvent.AccountCreated).username)
        Assert.assertNull(settings.authToken)
    }

    @Test
    fun signUp_whenSessionStoreFails_reportsSignUpOperation() = runTest {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson(tokenResponse("token-1", "Satoshi"))

        val driver = FailingDriver()
        val failingDb = Database(driver, ":memory:")
        val failingSettings = Settings(
            dbProvider = { failingDb },
            legacyValues = { emptyMap() },
        )
        driver.failing = true

        val model = AuthViewModel(api(), failingDb, failingSettings)
        model.signUp("Satoshi", "SuperSecure", Bundle())

        val event = model.events.first()

        Assert.assertTrue(event is AuthEvent.Failed)
        Assert.assertEquals(AuthOperation.SignUp, (event as AuthEvent.Failed).operation)
    }

    @Test
    fun signIn_whileRequestInFlight_isIgnored() = runTest {
        enqueueJson(tokenResponse("token-1", "satoshi"))

        val model = viewModel()
        model.signIn("satoshi", "SuperSecure", Bundle())
        model.signIn("satoshi", "SuperSecure", Bundle())

        model.events.first()

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun busy_isRaisedWhileInFlightAndClearedAfter() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(tokenResponse("token-1", "satoshi"))
                .bodyDelay(200, TimeUnit.MILLISECONDS)
                .build()
        )

        val model = viewModel()
        model.signIn("satoshi", "SuperSecure", Bundle())

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
                .body(tokenResponse("token-1", "satoshi"))
                .bodyDelay(5, TimeUnit.SECONDS)
                .build()
        )

        val model = viewModel()
        model.signIn("satoshi", "SuperSecure", Bundle())
        model.busy.first { it }

        model.cancel()
        model.busy.first { !it }

        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun factory_createsAnAuthViewModel() {
        val model = AuthViewModel.Factory(api(), db, settings)
            .create(AuthViewModel::class.java)

        Assert.assertNotNull(model)
    }

    @Test
    fun factory_rejectsAnUnrelatedViewModelClass() {
        val factory = AuthViewModel.Factory(api(), db, settings)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnrelatedViewModel::class.java)
        }
    }
}

private class UnrelatedViewModel : ViewModel()

/** A [SQLiteDriver] that can be made to fail on the next statement. */
private class FailingDriver : SQLiteDriver {
    @Volatile
    var failing = false

    private val delegate = BundledSQLiteDriver()

    override fun open(fileName: String): SQLiteConnection {
        val connection = delegate.open(fileName)
        return object : SQLiteConnection {
            override fun prepare(sql: String): SQLiteStatement {
                if (failing) throw RuntimeException("database unavailable")
                return connection.prepare(sql)
            }

            override fun close() {
                connection.close()
            }
        }
    }
}
