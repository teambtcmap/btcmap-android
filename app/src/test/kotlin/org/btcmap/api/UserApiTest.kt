package org.btcmap.api

import kotlinx.coroutines.test.runTest
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
        Assert.assertEquals(1, user.roles.size())
        Assert.assertEquals(0, user.savedPlaces.size())
        Assert.assertEquals(0, user.savedAreas.size())
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
        Assert.assertEquals(1, user.savedPlaces.size())
        Assert.assertEquals(1, user.savedAreas.size())
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
}
