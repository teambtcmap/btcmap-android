package org.btcmap.map

import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.place.PlaceFragment
import org.btcmap.search.SearchAdapterItem
import org.btcmap.ui.SearchOverlayView
import org.btcmap.settings.mapViewport
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapPlaceSearchTest : AppTestCase() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun tappingPlaceResult_loadsThePlaceFromTheCache_andOpensTheBottomSheet() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var mapFragment: MapFragment
            val name = ParisPlaces.target.name.orEmpty()

            scenario.onActivity {
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                mapFragment.requireView().findViewById<SearchOverlayView>(R.id.search).results =
                    listOf(
                        SearchAdapterItem.Place(
                            placeId = ParisPlaces.target.id,
                            icon = ParisPlaces.target.icon,
                            name = name,
                            distanceToUser = null,
                            boosted = false,
                        )
                    )
            }

            composeTestRule.onNodeWithText(name).performClick()

            // The row tap looks the place up on a background dispatcher and only
            // then opens the sheet, so this also covers that path.
            waitUntilOnMain {
                BottomSheetBehavior.from(mapFragment.requireView().findViewById(R.id.placeBottomSheet)).state ==
                    BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            val placeFragment = mapFragment.childFragmentManager
                .findFragmentById(R.id.placeFragment) as PlaceFragment
            val title = placeFragment.requireView()
                .findViewById<Toolbar>(R.id.toolbar).title

            Assert.assertEquals(ParisPlaces.target.name, title)
        }
    }
}
