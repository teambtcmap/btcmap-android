package org.btcmap.api

import kotlinx.coroutines.test.runTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.btcmap.util.toUrl
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
                "saved_areas": [{"id": 2, "name": "Downtown"}],
                "geofence": [3, 7]
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
        Assert.assertEquals(listOf(3L, 7L), user.geofence)
    }

    @Test
    fun searchUsers_sendsQueryAndLimitAndParsesResults() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 124,
                    "name": "natinfosec",
                    "roles": ["user"],
                    "created_at": "2024-01-01T00:00:00Z",
                    "geofence": [3, 7],
                    "npub": "npub1..."
                }
            ]
            """.trimIndent()
        )

        val users = api().searchUsers("na", limit = 50)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/users", request.url.encodedPath)
        Assert.assertEquals("na", request.url.queryParameter("query"))
        Assert.assertEquals("50", request.url.queryParameter("limit"))
        Assert.assertEquals(1, users.size)
        Assert.assertEquals(124L, users.single().id)
        Assert.assertEquals("natinfosec", users.single().name)
        Assert.assertEquals(listOf("user"), users.single().roles)
        Assert.assertEquals("2024-01-01T00:00:00Z", users.single().createdAt)
        Assert.assertEquals(listOf(3L, 7L), users.single().geofence)
        Assert.assertEquals("npub1...", users.single().npub)
    }

    @Test
    fun searchUsers_toleratesMissingNpubAndEmptyGeofence() = runTest {
        enqueueJson(
            """[{"id":1,"name":"a","roles":[],"created_at":"2024-01-01T00:00:00Z","geofence":[]}]"""
        )

        val users = api().searchUsers("a")

        Assert.assertNull(users.single().npub)
        Assert.assertTrue(users.single().roles.isEmpty())
        Assert.assertTrue(users.single().geofence.isEmpty())
    }

    @Test
    fun updateUser_patchesRoles() = runTest {
        enqueueJson(
            """
            {
                "id": 124,
                "name": "natinfosec",
                "roles": ["user", "area_manager"],
                "created_at": "2024-01-01T00:00:00Z",
                "geofence": []
            }
            """.trimIndent()
        )

        val user = api().updateUser(124, roles = listOf("user", "area_manager"))

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/users/124", request.url.encodedPath)
        Assert.assertEquals("""{"roles":["user","area_manager"]}""", request.jsonBody())
        Assert.assertEquals(listOf("user", "area_manager"), user.roles)
    }

    @Test
    fun updateUser_patchesGeofence() = runTest {
        enqueueJson(
            """
            {
                "id": 124,
                "name": "natinfosec",
                "roles": ["user"],
                "created_at": "2024-01-01T00:00:00Z",
                "geofence": [3, 7]
            }
            """.trimIndent()
        )

        val user = api().updateUser(124, geofence = listOf(3, 7))

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/users/124", request.url.encodedPath)
        Assert.assertEquals("""{"geofence":[3,7]}""", request.jsonBody())
        Assert.assertEquals(listOf(3L, 7L), user.geofence)
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
            httpClient = HttpClient(CIO),
            baseUrl = { server.url("/").toString().toUrl() },
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
            httpClient = HttpClient(CIO),
            baseUrl = { server.url("/").toString().toUrl() },
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
