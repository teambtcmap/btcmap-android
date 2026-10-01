package org.btcmap.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test

class UserApiTest : ApiTestBase() {
    @Test
    fun createUser_postsNameAndPassword() = runTest {
        enqueueJson("""{"id":124,"name":"Satoshi","roles":["user"]}""")

        val user = api().createUser(name = "Satoshi", password = "SuperSecurePassword")

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/users", request.url.encodedPath)
        Assert.assertEquals(
            """{"password":"SuperSecurePassword","name":"Satoshi"}""",
            request.jsonBody(),
        )
        Assert.assertEquals(124L, user.id)
        Assert.assertEquals("Satoshi", user.name)
        Assert.assertEquals(listOf("user"), user.roles)
        Assert.assertEquals(0, user.savedPlaces.size)
        Assert.assertEquals(0, user.savedAreas.size)
    }

    @Test
    fun createUser_omitsBlankName() = runTest {
        enqueueJson("""{"id":124,"name":"generated","roles":["user"]}""")

        api().createUser(name = "  ", password = "pw")

        val request = takeRequest()
        Assert.assertEquals("""{"password":"pw"}""", request.jsonBody())
    }

    @Test
    fun getUser_parsesSavedPlacesAndAreas() = runTest {
        enqueueJson(
            """
            {
                "id": 1,
                "name": "satoshi",
                "roles": ["user"],
                "saved_places": [{"id": 1, "name": "Bitcoin Cafe"}],
                "saved_areas": [{"id": 2, "name": "Downtown"}]
            }
            """.trimIndent()
        )

        val user = api().getUser()

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/users/me", request.url.encodedPath)
        Assert.assertEquals(1, user.savedPlaces.size)
        Assert.assertEquals(1, user.savedAreas.size)
        Assert.assertEquals(1L, user.savedPlaces.single().id)
        Assert.assertEquals("Bitcoin Cafe", user.savedPlaces.single().name)
    }

    @Test
    fun updateUsername_putsAndParsesUser() = runTest {
        enqueueJson(
            """
            {
                "id": 124,
                "name": "newSatoshi",
                "roles": ["user"],
                "saved_places": [],
                "saved_areas": []
            }
            """.trimIndent()
        )

        val user = api().updateUsername("newSatoshi")

        val request = takeRequest()
        Assert.assertEquals("PUT", request.method)
        Assert.assertEquals("/v4/users/me/username", request.url.encodedPath)
        Assert.assertEquals("""{"username":"newSatoshi"}""", request.jsonBody())
        Assert.assertEquals("newSatoshi", user.name)
    }

    @Test
    fun updatePassword_putsPasswordChange() = runTest {
        enqueueJson("{}")

        api().updatePassword(oldPassword = "old", newPassword = "new")

        val request = takeRequest()
        Assert.assertEquals("PUT", request.method)
        Assert.assertEquals("/v4/users/me/password", request.url.encodedPath)
        Assert.assertEquals(
            """{"old_password":"old","new_password":"new"}""",
            request.jsonBody(),
        )
    }

    @Test
    fun signIn_postsTokenRequestWithPasswordHeader() = runTest {
        enqueueJson(
            """
            {
                "token": "token-1",
                "user": {
                    "id": 1,
                    "name": "satoshi",
                    "roles": ["user"],
                    "saved_places": [],
                    "saved_areas": []
                }
            }
            """.trimIndent()
        )

        val response = api().signIn(username = "satoshi", password = "pw", label = "device")

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/users/satoshi/tokens", request.url.encodedPath)
        Assert.assertEquals("Bearer pw", request.headers["Authorization"])
        Assert.assertEquals("""{"label":"device"}""", request.jsonBody())
        Assert.assertEquals("token-1", response.token)
        Assert.assertEquals("satoshi", response.user.name)
    }

    @Test
    fun signIn_rejectsBlankToken() = runTest {
        enqueueJson(
            """
            {
                "token": "",
                "user": {
                    "id": 1,
                    "name": "satoshi",
                    "roles": ["user"],
                    "saved_places": [],
                    "saved_areas": []
                }
            }
            """.trimIndent()
        )

        try {
            api().signIn(username = "satoshi", password = "pw", label = "device")
            Assert.fail("Expected ApiParseException")
        } catch (e: ApiParseException) {
            Assert.assertTrue(e.message!!.contains("token"))
        }
    }

    @Test
    fun signIn_doesNotClearSessionOnUnauthorized() = runTest {
        enqueueJson("""{"message":"Invalid credentials"}""", code = 401)

        var unauthorized = false
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { unauthorized = true },
        )

        try {
            api.signIn(username = "satoshi", password = "wrong", label = "device")
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertFalse(unauthorized)
    }

    @Test
    fun signOut_postsToRestEndpointWithExplicitToken() = runTest {
        enqueueJson("""{"id":42,"label":"device","revoked_at":"2026-09-17T12:34:56.789Z"}""")

        api().signOut("token-1")

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/auth/signout", request.url.encodedPath)
        Assert.assertEquals("Bearer token-1", request.headers["Authorization"])
    }

    @Test
    fun signOut_doesNotClearSessionOnUnauthorized() = runTest {
        enqueueJson("""{"error":{"code":1,"message":"Invalid bearer token"}}""", code = 401)

        var unauthorized = false
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { unauthorized = true },
        )

        try {
            api.signOut("stale-token")
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertFalse(unauthorized)
    }
}
