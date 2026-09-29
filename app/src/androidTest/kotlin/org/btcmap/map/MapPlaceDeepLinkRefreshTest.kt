package org.btcmap.map

import android.content.Intent
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.SyncEvent
import org.btcmap.db.table.place.Place
import org.btcmap.place.PlaceFragment
import org.btcmap.util.AppTestCase
import org.btcmap.util.TestSyncController
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

/**
 * A merchant deep link is opened from a chat room that tracks every OpenStreetMap
 * change, so the row it points at is often one the app has already synced in an
 * older version. The screen reads the cached row at once; once the launch sync
 * finishes and rewrites it, the screen has to refresh instead of leaving the
 * stale copy up.
 */
@RunWith(AndroidJUnit4::class)
class MapPlaceDeepLinkRefreshTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun deepLinkedPlace_refreshesWhenTheSyncUpdatesIt() {
        val placeId = 7L
        databaseRule.db.place.insert(listOf(place(id = placeId, name = "Old name")))

        val intent = Intent(Intent.ACTION_VIEW, "https://btcmap.org/merchant/$placeId".toUri())
            .setClass(app, Activity::class.java)

        ActivityScenario.launch<Activity>(intent).use { scenario ->
            lateinit var mapFragment: MapFragment
            scenario.onActivity {
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
            }

            val placeFragment = mapFragment.childFragmentManager
                .findFragmentById(R.id.placeFragment) as PlaceFragment

            waitUntilOnMain {
                sheetState(mapFragment) == BottomSheetBehavior.STATE_HALF_EXPANDED &&
                    title(placeFragment) == "Old name"
            }

            // The app-scoped sync rewrites the row that the deep link already
            // rendered and publishes the change to anything that observes it.
            databaseRule.db.place.insert(listOf(place(id = placeId, name = "New name")))
            scenario.onActivity {
                (app.syncControllerForTesting as TestSyncController).emit(SyncEvent.PlacesChanged)
            }

            waitUntilOnMain { title(placeFragment) == "New name" }

            Assert.assertEquals("New name", title(placeFragment))
        }
    }

    private fun title(fragment: PlaceFragment): CharSequence? {
        return fragment.requireView().findViewById<Toolbar>(R.id.toolbar).title
    }

    private fun sheetState(fragment: MapFragment): Int {
        return BottomSheetBehavior.from(
            fragment.requireView().findViewById(R.id.placeBottomSheet)
        ).state
    }

    private fun place(id: Long, name: String): Place {
        return Place(
            id = id,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "storefront",
            name = name,
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
}
