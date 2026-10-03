package org.btcmap.area

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.event.EventFragment
import org.btcmap.ui.AREA_BOOSTED_CARD_TAG
import org.btcmap.ui.AREA_BOOSTED_TITLE_TAG
import org.btcmap.ui.AREA_DESCRIPTION_TAG
import org.btcmap.ui.AREA_EVENTS_TITLE_TAG
import org.btcmap.ui.AREA_EVENT_CARD_TAG
import org.btcmap.ui.AREA_ISSUES_TITLE_TAG
import org.btcmap.ui.AREA_ISSUE_CARD_TAG
import org.btcmap.ui.AREA_READ_MORE_TAG
import org.btcmap.ui.AREA_WEBSITE_TAG
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class AreaContentTest : AreaScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun loadSuccess_showsTitleDescriptionAndWebsite() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area(description = "Greater Paris")))

        withArea { scenario, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity {
                Assert.assertEquals(
                    "Grand Paris",
                    area.view().findViewById<Toolbar>(R.id.toolbar).title,
                )
                Assert.assertTrue(area.view().findViewById<View>(R.id.content).isVisible)
            }

            composeTestRule.waitUntil { hasTag(AREA_DESCRIPTION_TAG) }
            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG)
                .assertTextContains("Greater Paris")
            composeTestRule.onNodeWithTag(AREA_WEBSITE_TAG)
                .assertTextContains("btcmap.org/community/grand-paris")
        }
    }

    @Test
    fun singleParagraphDescription_hidesReadMoreToggle() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area(description = "Greater Paris")))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }
            composeTestRule.waitUntil { hasTag(AREA_DESCRIPTION_TAG) }

            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun multiParagraphDescription_canBeExpandedAndCollapsed() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(description = "First paragraph.\n\nSecond paragraph.")),
        )

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }
            composeTestRule.waitUntil { hasTag(AREA_READ_MORE_TAG) }

            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG)
                .assertTextContains("First paragraph.")
            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).assertTextContains("Read more")

            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).performClick()
            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG)
                .assertTextContains("First paragraph.\n\nSecond paragraph.")
            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).assertTextContains("Collapse")

            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).performClick()
            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG)
                .assertTextContains("First paragraph.")
            composeTestRule.onNodeWithTag(AREA_READ_MORE_TAG).assertTextContains("Read more")
        }
    }

    @Test
    fun upcomingEvents_areSortedAscendingAndPastEventsHidden() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.event.insert(
            listOf(
                event(id = 1, name = "Past", startsAt = "2020-01-01T10:00:00Z"),
                event(id = 2, name = "Later", startsAt = "2999-01-02T10:00:00Z"),
                event(id = 3, name = "Sooner", startsAt = "2999-01-01T10:00:00Z"),
            ),
        )

        withArea { _, area ->
            composeTestRule.waitUntil { eventCount() == 2 }

            eventCards()[0].assertTextContains("Sooner")
            eventCards()[1].assertTextContains("Later")
            composeTestRule.onNodeWithTag(AREA_EVENTS_TITLE_TAG).assertExists()
        }
    }

    @Test
    fun noUpcomingEvents_hidesEventsSection() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            composeTestRule.onNodeWithTag(AREA_EVENTS_TITLE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun clickingUpcomingEvent_opensEventScreen() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.event.insert(
            listOf(event(id = 5, name = "Meetup", startsAt = "2999-01-01T10:00:00Z")),
        )

        withArea { scenario, area ->
            composeTestRule.waitUntil { eventCount() == 1 }

            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            composeTestRule.onNodeWithTag(AREA_EVENT_CARD_TAG).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is EventFragment
            }
        }
    }

    @Test
    fun placeIssues_renderCardsAndTruncatedCount() {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = issuesJson(
                total = 5,
                issueJson("node", 987654321, "Bitcoin ATM", "missing_icon"),
                issueJson("way", 456789123, "Satoshi's Pub", "outdated"),
            ),
        )
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.place.insert(listOf(place(osmId = "node:987654321", icon = "local_atm")))

        withArea { _, area ->
            composeTestRule.waitUntil { issueCount() == 2 }

            composeTestRule.onNodeWithTag(AREA_ISSUES_TITLE_TAG)
                .assertTextContains("Issues (2 of 5)")
            issueCards()[0].assertTextContains("Bitcoin ATM")
            issueCards()[0].assertTextContains("Missing icon")
            issueCards()[1].assertTextContains("Outdated, needs verification")
        }
    }

    @Test
    fun placeIssues_fullCountHasNoTruncationSuffix() {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = issuesJson(
                total = 1,
                issueJson("node", 1, "Shop", "not_verified"),
            ),
        )
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            composeTestRule.waitUntil { issueCount() == 1 }

            composeTestRule.onNodeWithTag(AREA_ISSUES_TITLE_TAG)
                .assertTextContains("Issues (1)")
            issueCards()[0].assertTextContains("Not verified")
        }
    }

    @Test
    fun placeIssues_parameterizedAndUnknownCodesAreRendered() {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = issuesJson(
                total = 2,
                issueJson("node", 1, "Shop", "invalid_tag_value:name"),
                issueJson("node", 2, "Other", "brand_new_code"),
            ),
        )
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            composeTestRule.waitUntil { issueCount() == 2 }

            issueCards()[0].assertTextContains("Invalid value for name")
            issueCards()[1].assertTextContains("Unknown issue")
        }
    }

    @Test
    fun noPlaceIssues_hidesIssuesSection() {
        apiRule.server.dispatcher = areaDispatcher(issuesBody = EMPTY_ISSUES_JSON)
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            composeTestRule.onNodeWithTag(AREA_ISSUES_TITLE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun headerImage_isHiddenWhenAreaHasNoIcon() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area(icon = null)))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            Assert.assertFalse(area.view().findViewById<View>(R.id.icon).isVisible)
        }
    }

    @Test
    fun headerImage_isShownWhenAreaHasIcon() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(icon = "https://example.com/icon.png")),
        )

        withArea { _, area ->
            waitUntilOnMain {
                !area.view().findViewById<View>(R.id.loading).isVisible
            }

            Assert.assertTrue(area.view().findViewById<View>(R.id.icon).isVisible)
        }
    }

    @Test
    fun boostedMerchants_renderOnlyActiveBoosts() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.place.insert(
            listOf(
                place(
                    osmId = "node:1",
                    icon = "cafe",
                    id = 1,
                    lat = 48.8566,
                    lon = 2.3522,
                    name = "Boosted Cafe",
                    boostedUntil = ZonedDateTime.parse("2999-01-01T00:00:00Z"),
                ),
                place(
                    osmId = "node:2",
                    icon = "cafe",
                    id = 2,
                    lat = 48.8566,
                    lon = 2.3522,
                    name = "Expired Cafe",
                    boostedUntil = ZonedDateTime.parse("2020-01-01T00:00:00Z"),
                ),
                place(
                    osmId = "node:3",
                    icon = "cafe",
                    id = 3,
                    lat = 48.8566,
                    lon = 2.3522,
                    name = "Plain Cafe",
                ),
            ),
        )

        withArea { _, area ->
            composeTestRule.waitUntil { boostedCount() == 1 }

            composeTestRule.onNodeWithTag(AREA_BOOSTED_TITLE_TAG).assertExists()
            boostedCards()[0].assertTextContains("Boosted Cafe")
        }
    }

    @Test
    fun noBoostedMerchants_hidesBoostedSection() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.place.insert(
            listOf(place(osmId = "node:1", icon = "cafe", lat = 48.8566, lon = 2.3522)),
        )

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            composeTestRule.onNodeWithTag(AREA_BOOSTED_TITLE_TAG).assertDoesNotExist()
        }
    }

    private fun hasTag(tag: String): Boolean =
        composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun eventCards() = composeTestRule.onAllNodesWithTag(AREA_EVENT_CARD_TAG)

    private fun eventCount() = eventCards().fetchSemanticsNodes().size

    private fun issueCards() = composeTestRule.onAllNodesWithTag(AREA_ISSUE_CARD_TAG)

    private fun issueCount() = issueCards().fetchSemanticsNodes().size

    private fun boostedCards() = composeTestRule.onAllNodesWithTag(AREA_BOOSTED_CARD_TAG)

    private fun boostedCount() = boostedCards().fetchSemanticsNodes().size

    private fun AreaFragment.view(): View = requireView()

    private fun place(
        osmId: String,
        icon: String,
        id: Long = 1,
        lat: Double = 0.0,
        lon: Double = 0.0,
        name: String = "Test Place",
        boostedUntil: ZonedDateTime? = null,
    ): Place {
        return Place(
            id = id,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = lat,
            lon = lon,
            icon = icon,
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
            boostedUntil = boostedUntil,
            comments = null,
            telegram = null,
            osmId = osmId,
        )
    }
}
