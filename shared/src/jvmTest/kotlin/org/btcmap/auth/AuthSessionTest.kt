package org.btcmap.auth

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.btcmap.api.ApiException
import org.btcmap.api.ApiTestBase
import org.btcmap.db.Database
import org.btcmap.db.testDatabase
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.junit.Assert
import org.junit.Test

/**
 * The session machinery behind the sign-in and sign-up forms: a real HTTP server
 * stands in for the API and an in-memory database for the store.
 *
 * These cases were ported from the Views auth view-model tests when the account
 * screens moved to shared Compose and the logic moved into [AuthSession]; the
 * view model's `busy`/`events` plumbing is gone, so the tests drive
 * [AuthSession.signIn] and [AuthSession.signUp] directly.
 *
 * Everything runs under [runBlocking] rather than `runTest`: [AuthSession] wraps
 * each request in a real `withTimeout`, which a virtual-time test dispatcher would
 * advance straight to the deadline before the response could arrive.
 */
class AuthSessionTest : ApiTestBase() {

    private fun settings(db: Database) =
        Settings(dbProvider = { db }, legacyValues = { emptyMap() })

    private fun tokenResponse(token: String, name: String): String =
        """{"token":"$token","user":{"id":1,"name":"$name","roles":["user"]}}"""

    @Test
    fun signIn_success_storesSessionAndReturnsAuthenticated() = runBlocking<Unit> {
        enqueueJson(tokenResponse("token-1", "satoshi"))
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signIn(api(), db, prefs, "satoshi", "pw", "device")

        Assert.assertTrue(outcome is AuthOutcome.Authenticated)
        Assert.assertEquals("satoshi", (outcome as AuthOutcome.Authenticated).name)
        Assert.assertEquals("token-1", prefs.authToken)
        Assert.assertEquals("satoshi", db.user.select()?.name)
    }

    @Test
    fun signIn_failure_returnsFailedAndStaysSignedOut() = runBlocking<Unit> {
        enqueueJson("""{"message":"Invalid credentials"}""", code = 401)
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signIn(api(), db, prefs, "satoshi", "wrong", "device")

        Assert.assertTrue(outcome is AuthOutcome.Failed)
        val error = (outcome as AuthOutcome.Failed).error
        Assert.assertTrue(error is ApiException)
        Assert.assertEquals(401, (error as ApiException).code)
        Assert.assertNull(prefs.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun signUp_success_signsInAndStoresSession() = runBlocking<Unit> {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson(tokenResponse("token-1", "Satoshi"))
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device")

        Assert.assertTrue(outcome is AuthOutcome.Authenticated)
        Assert.assertEquals("Satoshi", (outcome as AuthOutcome.Authenticated).name)
        Assert.assertEquals("token-1", prefs.authToken)
    }

    @Test
    fun signUp_whenAutoSignInFails_reportsAccountCreated() = runBlocking<Unit> {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson("""{"message":"boom"}""", code = 500)
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device")

        Assert.assertTrue(outcome is AuthOutcome.AccountCreated)
        Assert.assertEquals("Satoshi", (outcome as AuthOutcome.AccountCreated).username)
        Assert.assertNull(prefs.authToken)
    }

    @Test
    fun signUp_whenCreateResponseIsLost_signsInAndStoresSession() = runBlocking<Unit> {
        // The server creates the account but the response is lost, so a failed
        // create cannot be told apart from one that succeeded. Signing in with
        // the same credentials proves the account exists and recovers the
        // session instead of sending a retry into "username already taken".
        enqueueJson("""{"message":"boom"}""", code = 500)
        enqueueJson(tokenResponse("token-1", "Satoshi"))
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device")

        Assert.assertTrue(outcome is AuthOutcome.Authenticated)
        Assert.assertEquals("Satoshi", (outcome as AuthOutcome.Authenticated).name)
        Assert.assertEquals("token-1", prefs.authToken)
        Assert.assertEquals("Satoshi", db.user.select()?.name)
        Assert.assertEquals(2, server.requestCount)
    }

    @Test
    fun signUp_whenCreateAndSignInBothFail_returnsCreateFailure() = runBlocking<Unit> {
        // Neither call succeeded, so no account exists and the creation error is
        // the one the user must see, not the follow-up sign-in failure.
        enqueueJson("""{"message":"Invalid password"}""", code = 400)
        enqueueJson("""{"message":"Invalid credentials"}""", code = 401)
        val db = testDatabase()
        val prefs = settings(db)

        val outcome = AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device")

        Assert.assertTrue(outcome is AuthOutcome.Failed)
        val error = (outcome as AuthOutcome.Failed).error
        Assert.assertTrue(error is ApiException)
        Assert.assertEquals(400, (error as ApiException).code)
        Assert.assertNull(prefs.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun signUp_whenSessionStoreFails_reportsAccountCreated() = runBlocking<Unit> {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        enqueueJson(tokenResponse("token-1", "Satoshi"))

        val driver = FailingDriver()
        val db = Database(driver, ":memory:")
        db.connect()
        val prefs = settings(db)
        driver.failing = true

        val outcome = AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device")

        // The account was created; only the local session could not be stored,
        // so this must not be reported as a failed account creation.
        Assert.assertTrue(outcome is AuthOutcome.AccountCreated)
        Assert.assertEquals("Satoshi", (outcome as AuthOutcome.AccountCreated).username)
        Assert.assertNull(prefs.authToken)
    }

    @Test
    fun signUp_cancelDuringAutoSignIn_reportsAccountCreated() = runBlocking<Unit> {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(tokenResponse("token-1", "Satoshi"))
                .bodyDelay(5, TimeUnit.SECONDS)
                .build()
        )
        val db = testDatabase()
        val prefs = settings(db)
        var created: String? = null

        // On a real dispatcher: the test thread blocks in `takeRequest` while the
        // sign-up runs, so the run itself must not share this thread.
        val job = launch(Dispatchers.Default) {
            AuthSession.signUp(api(), db, prefs, "Satoshi", "pw", "device") { created = it }
        }

        // The account was created and the follow-up sign-in is in flight; cancel
        // mid-request: the account must still be reported.
        server.takeRequest()
        server.takeRequest()
        job.cancelAndJoin()

        Assert.assertEquals("Satoshi", created)
        Assert.assertNull(prefs.authToken)
    }
}

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
