package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

/**
 * Every request to the configured API carries the stored session token so the
 * server can prioritize signed-in users. Account creation is the one exception
 * (it runs before a session exists); sign-in carries the password instead.
 */
class ApiAuthorizationTest : ApiTestBase() {

    private fun tokenApi(): Api = Api(
        httpClient = httpClient(),
        baseUrl = { baseUrl() },
        token = { "token-1" },
    )

    private suspend fun assertAuthorized(block: suspend () -> Unit) {
        block()
        Assert.assertEquals("Bearer token-1", takeRequest().headers["Authorization"])
    }

    @Test
    fun readEndpointsAttachStoredToken() = runTest {
        val api = tokenApi()

        enqueueJson("[]")
        assertAuthorized { api.getPlaces(updatedSince = null, limit = 5) }

        enqueueJson("""{"lat":1.0,"lon":2.0}""")
        assertAuthorized { api.getPlaceCoordinates(5) }

        enqueueJson("""{"osm_id":"node:5"}""")
        assertAuthorized { api.getPlaceOsmId(5) }

        enqueueJson("[]")
        assertAuthorized { api.getPlaceImages(5) }

        enqueueJson("[]")
        assertAuthorized { api.getComments(updatedSince = null, limit = 5) }

        enqueueJson("""{"quote_sat":1}""")
        assertAuthorized { api.getCommentQuote() }

        enqueueJson("""{"quote_30d_sat":1,"quote_90d_sat":1,"quote_365d_sat":1}""")
        assertAuthorized { api.getPlaceBoostQuote() }

        enqueueJson("""{"id":"i","status":"unpaid"}""")
        assertAuthorized { api.getInvoice("i") }

        enqueueJson("[]")
        assertAuthorized { api.getActivity(areaIds = listOf("1")) }

        enqueueJson("[]")
        assertAuthorized { api.getAreas(updatedSince = Instant.parse("2026-01-01T00:00:00Z"), limit = 5) }

        enqueueJson("""{"id":1,"name":"n","type":"t","url_alias":"a","website_url":"https://x"}""")
        assertAuthorized { api.getArea("1", lang = "en") }

        enqueueJson("[]")
        assertAuthorized { api.getEvents(updatedSince = Instant.parse("2026-01-01T00:00:00Z"), limit = 5) }

        enqueueJson("""{"id":1,"lat":1.0,"lon":2.0,"name":"n","starts_at":"2026-01-01T00:00:00Z"}""")
        assertAuthorized { api.getEvent(1) }

        enqueueJson("""{"total_issues":0,"requested_issues":[]}""")
        assertAuthorized { api.getPlaceIssues(areaId = 1) }
    }

    @Test
    fun writeEndpointsAttachStoredToken() = runTest {
        val api = tokenApi()

        enqueueJson("""{"invoice_id":"i","invoice":"l"}""")
        assertAuthorized { api.addComment(placeId = 5, comment = "x") }

        enqueueJson("""{"invoice_id":"i","invoice":"l"}""")
        assertAuthorized { api.boostPlace(placeId = 5, days = 30) }
    }

    @Test
    fun rejectedStoredTokenOnReadEndpointTriggersSessionClearing() = runTest {
        enqueueJson("""{"message":"Authentication required"}""", code = 401)

        var rejectedToken: String? = null
        val api = Api(
            httpClient = httpClient(),
            baseUrl = { baseUrl() },
            token = { "stale-token" },
            onUnauthorized = { rejectedToken = it },
        )

        try {
            api.getPlaces(updatedSince = null, limit = 5)
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertEquals("stale-token", rejectedToken)
    }

    @Test
    fun createUserDoesNotAttachStoredToken() = runTest {
        enqueueJson("""{"id":1,"name":"n","roles":["user"]}""")

        tokenApi().createUser(name = "n", password = "pw")

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun signInSendsPasswordInsteadOfStoredToken() = runTest {
        enqueueJson(
            """
            {"token":"new","user":{"id":1,"name":"n","roles":["user"],"saved_places":[],"saved_areas":[]}}
            """.trimIndent()
        )

        tokenApi().signIn(username = "n", password = "pw", label = "device")

        Assert.assertEquals("Bearer pw", takeRequest().headers["Authorization"])
    }
}
