package org.btcmap.place

import androidx.appcompat.widget.Toolbar
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
import org.btcmap.settings.authToken
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.assertNoUncaughtException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class PlaceSaveErrorHandlingTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun load_whenUserMissingFromDatabase_doesNotLeakUncaughtException() {
        preferencesRule.prefs.authToken = "test-token"

        withPlaceFragment { }
    }

    @Test
    fun save_whenApiFails_doesNotLeakUncaughtException() {
        preferencesRule.prefs.authToken = "test-token"
        databaseRule.db.user.insert(user())

        withPlaceFragment { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentByTag(PLACE_TAG) as PlaceFragment
            val toolbar = fragment.requireView().findViewById<Toolbar>(R.id.toolbar)
            toolbar.menu.performIdentifierAction(R.id.save, 0)
        }
    }

    private fun withPlaceFragment(action: (Activity) -> Unit) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                assertNoUncaughtException(
                    "Exception escaped the place screen's coroutine to the uncaught handler",
                ) {
                    scenario.onActivity { activity ->
                        val place = PlaceFragment()
                        activity.supportFragmentManager.commitNow {
                            setReorderingAllowed(true)
                            replace(R.id.fragmentContainerView, place, PLACE_TAG)
                        }
                        place.setPlace(placeRow())
                        action(activity)
                    }
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun placeRow(): Place {
        return Place(
            id = 1,
            bundled = false,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "storefront",
            name = "Test Merchant",
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            localizedOpeningHours = null,
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
