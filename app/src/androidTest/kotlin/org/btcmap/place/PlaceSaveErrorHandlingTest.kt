package org.btcmap.place

import org.btcmap.i18n.Strings

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
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.user.User
import org.btcmap.nav.AppRootFragment
import org.btcmap.settings.authToken
import org.btcmap.ui.AppRoute
import org.btcmap.ui.PLACE_MENU_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.assertNoUncaughtException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class PlaceSaveErrorHandlingTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun load_whenUserMissingFromDatabase_doesNotLeakUncaughtException() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.place.insert(listOf(placeRow()))

        withPlaceFragment { }
    }

    @Test
    fun save_whenApiFails_doesNotLeakUncaughtException() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user())
        databaseRule.db.place.insert(listOf(placeRow()))

        withPlaceFragment {
            awaitPlace()
            composeTestRule.onNodeWithTag(PLACE_MENU_TAG).performClick()
            composeTestRule.onNodeWithText(Strings.current()["save"]).performClick()
        }
    }

    private fun withPlaceFragment(action: () -> Unit) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                assertNoUncaughtException(
                    "Exception escaped the place screen's coroutine to the uncaught handler",
                ) {
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
                    action()
                }
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

    private fun user(): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = emptyList(),
            savedAreas = emptyList(),
        )
    }

    companion object {
        private const val PLACE_TAG = "place"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
