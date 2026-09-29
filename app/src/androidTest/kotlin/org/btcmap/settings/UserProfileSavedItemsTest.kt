package org.btcmap.settings

import android.view.View
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * The profile screen's saved lists. A delete applies the canonical id list the
 * endpoint returns instead of refetching `/users/me`, and only falls back to a
 * refetch when the server names an item the cache cannot render.
 */
@RunWith(AndroidJUnit4::class)
class UserProfileSavedItemsTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun deleteSavedPlace_updatesListFromResponseWithoutRefetchingUser() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedPlaces = listOf(1L to "One", 2L to "Two")))
        val userFetches = AtomicInteger(0)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath == "/v4/users/me" -> {
                        userFetches.incrementAndGet()
                        jsonResponse(
                            """{"id":1,"name":"tester","roles":[],"saved_places":[],"saved_areas":[]}"""
                        )
                    }

                    request.url.encodedPath == "/v4/places/saved/1" &&
                        request.method == "DELETE" -> jsonResponse("[2]")

                    else -> jsonResponse("[]")
                }
            }
        }

        withProfile { scenario, profile ->
            val list = profile.requireView()
                .findViewById<RecyclerView>(R.id.savedPlacesList)
            waitUntilOnMain { (list.adapter?.itemCount ?: 0) == 2 && list.getChildAt(0) != null }

            scenario.onActivity {
                list.getChildAt(0).findViewById<View>(R.id.deleteButton).performClick()
            }

            waitUntil { savedPlaceIds() == listOf(2L) }
            waitUntilOnMain { list.adapter?.itemCount == 1 }
            Assert.assertEquals(0, userFetches.get())
        }
    }

    @Test
    fun deleteSavedArea_updatesListFromResponse() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedAreas = listOf(10L to "Grand Paris", 11L to "Other")))
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath == "/v4/areas/saved/10" &&
                        request.method == "DELETE" -> jsonResponse("[11]")

                    else -> jsonResponse("[]")
                }
            }
        }

        withProfile { scenario, profile ->
            val list = profile.requireView()
                .findViewById<RecyclerView>(R.id.savedAreasList)
            waitUntilOnMain { (list.adapter?.itemCount ?: 0) == 2 && list.getChildAt(0) != null }

            scenario.onActivity {
                list.getChildAt(0).findViewById<View>(R.id.deleteButton).performClick()
            }

            waitUntil { savedAreaIds() == listOf(11L) }
            waitUntilOnMain { list.adapter?.itemCount == 1 }
        }
    }

    @Test
    fun deleteSavedPlace_whenServerReturnsUnknownId_refetchesUser() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedPlaces = listOf(1L to "One")))
        val userFetches = AtomicInteger(0)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath == "/v4/users/me" -> {
                        userFetches.incrementAndGet()
                        jsonResponse(
                            """{"id":1,"name":"tester","roles":[],"saved_places":[{"id":2,"name":"Other"}],"saved_areas":[]}"""
                        )
                    }

                    request.url.encodedPath == "/v4/places/saved/1" &&
                        request.method == "DELETE" -> jsonResponse("[2]")

                    else -> jsonResponse("[]")
                }
            }
        }

        withProfile { scenario, profile ->
            val list = profile.requireView()
                .findViewById<RecyclerView>(R.id.savedPlacesList)
            waitUntilOnMain { (list.adapter?.itemCount ?: 0) == 1 && list.getChildAt(0) != null }

            scenario.onActivity {
                list.getChildAt(0).findViewById<View>(R.id.deleteButton).performClick()
            }

            waitUntil { savedPlaceIds() == listOf(2L) }
            waitUntil { userFetches.get() == 1 }
            Assert.assertEquals("Other", databaseRule.db.user.select()?.savedPlaces?.single()?.name)
        }
    }

    private fun withProfile(block: (ActivityScenario<Activity>, UserProfileFragment) -> Unit) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var profile: UserProfileFragment
                scenario.onActivity { activity ->
                    profile = UserProfileFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, profile, PROFILE_TAG)
                    }
                }
                block(scenario, profile)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun savedPlaceIds(): List<Long> {
        return databaseRule.db.user.select()?.savedPlaces?.map { it.id }.orEmpty()
    }

    private fun savedAreaIds(): List<Long> {
        return databaseRule.db.user.select()?.savedAreas?.map { it.id }.orEmpty()
    }

    private fun user(
        savedPlaces: List<Pair<Long, String>> = emptyList(),
        savedAreas: List<Pair<Long, String>> = emptyList(),
    ): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = savedPlaces.map { SavedItem(id = it.first, name = it.second) },
            savedAreas = savedAreas.map { SavedItem(id = it.first, name = it.second) },
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
        const val PROFILE_TAG = "profile"
        const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
