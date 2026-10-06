package org.btcmap.place

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.nav.AppRootFragment
import org.btcmap.settings.authToken
import org.btcmap.ui.ACCOUNT_USERNAME_TAG
import org.btcmap.ui.AppRoute
import org.btcmap.ui.PLACE_MENU_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The place screen's bookmark adds, removes and, when signed out, opens the auth
 * dialog instead of calling the API.
 */
@RunWith(AndroidJUnit4::class)
class PlaceBookmarkTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun save_whenAuthorized_postsToApiAndPersistsPlace() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedPlaceIds = emptyList()))
        databaseRule.db.place.insert(listOf(placeRow()))
        val posted = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/v4/places/saved" && request.method == "POST" -> {
                        posted.set(true)
                        jsonResponse("[1]")
                    }

                    else -> jsonResponse("[]")
                }
            }
        }

        withPlace {
            save()

            waitUntil { posted.get() }
            waitUntil { savedPlaceIds().contains(1L) }
        }
    }

    @Test
    fun save_whenAlreadySaved_deletesFromApiAndPersistsChange() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedPlaceIds = listOf(1)))
        databaseRule.db.place.insert(listOf(placeRow()))
        val deleted = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/v4/places/saved/1" && request.method == "DELETE" -> {
                        deleted.set(true)
                        jsonResponse("[]")
                    }

                    else -> jsonResponse("[]")
                }
            }
        }

        withPlace {
            save()

            waitUntil { deleted.get() }
            waitUntil { savedPlaceIds().isEmpty() }
        }
    }

    @Test
    fun save_whenUnauthorized_showsAuthDialogAndDoesNotCallApi() = runBlocking<Unit> {
        databaseRule.db.place.insert(listOf(placeRow()))
        val saving = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath.startsWith("/v4/places/saved")) {
                    saving.set(true)
                }
                return jsonResponse("[]")
            }
        }

        withPlace {
            save()

            composeTestRule.waitUntil(5_000) {
                try {
                    composeTestRule.onNodeWithTag(ACCOUNT_USERNAME_TAG).assertExists()
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            Assert.assertFalse(saving.get())
        }
    }

    private fun withPlace(block: (ActivityScenario<Activity>) -> Unit) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(
                            R.id.fragmentContainerView,
                            AppRootFragment.create(AppRoute.Place(1L)),
                            PLACE_TAG,
                        )
                    }
                }
                awaitPlace()
                block(scenario)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun awaitPlace() {
        composeTestRule.waitUntil(5_000) {
            try {
                composeTestRule.onNodeWithTag(PLACE_MENU_TAG).assertExists()
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun save() {
        composeTestRule.onNodeWithTag(PLACE_MENU_TAG).performClick()
        composeTestRule.onNodeWithText(app.getString(R.string.save)).performClick()
    }

    private fun savedPlaceIds(): List<Long> {
        return runBlocking { databaseRule.db.user.select() }
            ?.savedPlaces
            ?.map { it.id }
            .orEmpty()
    }

    private fun user(savedPlaceIds: List<Long>): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = savedPlaceIds.map { SavedItem(id = it, name = "Test Merchant") },
            savedAreas = emptyList(),
        )
    }

    private fun placeRow(): Place {
        return Place(
            id = 1,
            updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "storefront",
            name = "Test Merchant",
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            phone = null,
            website = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            requiredAppUrl = null,
            boostedUntil = null,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }

    private fun jsonResponse(body: String, code: Int = 200): MockResponse {
        return MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    }

    private companion object {
        const val PLACE_TAG = "place"
        const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
