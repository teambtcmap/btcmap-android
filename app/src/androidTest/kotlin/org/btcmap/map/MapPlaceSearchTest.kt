package org.btcmap.map

import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.search.SearchView
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.place.PlaceFragment
import org.btcmap.search.SearchAdapter
import org.btcmap.search.SearchAdapterItem
import org.btcmap.settings.mapViewport
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapPlaceSearchTest : AppTestCase() {

    @Test
    fun tappingPlaceResult_loadsThePlaceFromTheCache_andOpensTheBottomSheet() {
        databaseRule.db.place.insert(ParisPlaces.places)
        preferencesRule.prefs.mapViewport = ParisPlaces.bounds

        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var mapFragment: MapFragment
            lateinit var results: RecyclerView
            lateinit var adapter: SearchAdapter

            scenario.onActivity {
                mapFragment = it.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as MapFragment
                results = mapFragment.requireView().findViewById(R.id.searchResults)
                adapter = results.adapter as SearchAdapter
                mapFragment.requireView()
                    .findViewById<SearchView>(R.id.searchView)
                    .show()
                adapter.submitList(
                    listOf(
                        SearchAdapterItem.Place(
                            placeId = ParisPlaces.target.id,
                            icon = ParisPlaces.target.icon,
                            name = ParisPlaces.target.name.orEmpty(),
                            distanceToUser = null,
                            boosted = false,
                        )
                    )
                )
            }

            waitUntilOnMain { results.childCount == 1 }
            scenario.onActivity { results.getChildAt(0).performClick() }

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
