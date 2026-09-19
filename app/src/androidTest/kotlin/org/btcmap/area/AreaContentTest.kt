package org.btcmap.area

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.event.EventFragment
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class AreaContentTest : AreaScreenTest() {

    @Test
    fun loadSuccess_showsTitleDescriptionAndWebsite() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area(description = "Greater Paris")))

        withArea { scenario, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity {
                val view = area.view()
                Assert.assertEquals(
                    "Grand Paris",
                    view.findViewById<Toolbar>(R.id.toolbar).title,
                )
                Assert.assertEquals(
                    "Greater Paris",
                    view.findViewById<TextView>(R.id.description).text.toString(),
                )
                Assert.assertEquals(
                    "btcmap.org/community/grand-paris",
                    view.findViewById<TextView>(R.id.website).text.toString(),
                )
                Assert.assertTrue(view.findViewById<View>(R.id.content).isVisible)
            }
        }
    }

    @Test
    fun singleParagraphDescription_hidesReadMoreToggle() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area(description = "Greater Paris")))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            Assert.assertFalse(
                area.view().findViewById<View>(R.id.description_expand).isVisible,
            )
        }
    }

    @Test
    fun multiParagraphDescription_canBeExpandedAndCollapsed() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(description = "First paragraph.\n\nSecond paragraph.")),
        )

        withArea { scenario, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity {
                val description = area.view().findViewById<TextView>(R.id.description)
                val expand = area.view().findViewById<Button>(R.id.description_expand)
                Assert.assertTrue(expand.isVisible)
                Assert.assertEquals("First paragraph.", description.text.toString())
                Assert.assertEquals("Read more", expand.text.toString())

                expand.performClick()
                Assert.assertEquals(
                    "First paragraph.\n\nSecond paragraph.",
                    description.text.toString(),
                )
                Assert.assertEquals("Collapse", expand.text.toString())

                expand.performClick()
                Assert.assertEquals("First paragraph.", description.text.toString())
                Assert.assertEquals("Read more", expand.text.toString())
            }
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

        withArea { scenario, area ->
            waitUntilOnMain { area.eventsContainer().childCount == 2 }

            scenario.onActivity {
                val container = area.eventsContainer()
                Assert.assertEquals(
                    "Sooner",
                    container.getChildAt(0).findViewById<TextView>(R.id.title).text.toString(),
                )
                Assert.assertEquals(
                    "Later",
                    container.getChildAt(1).findViewById<TextView>(R.id.title).text.toString(),
                )
                Assert.assertTrue(area.view().findViewById<View>(R.id.events_title).isVisible)
            }
        }
    }

    @Test
    fun noUpcomingEvents_hidesEventsSection() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            Assert.assertFalse(area.view().findViewById<View>(R.id.events_title).isVisible)
            Assert.assertFalse(area.eventsContainer().isVisible)
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
            waitUntilOnMain { area.eventsContainer().childCount == 1 }

            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            scenario.onActivity {
                area.eventsContainer().getChildAt(0).performClick()
            }

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

        withArea { scenario, area ->
            waitUntilOnMain { area.issuesContainer().childCount == 2 }

            scenario.onActivity {
                Assert.assertEquals(
                    "Issues (2 of 5)",
                    area.view().findViewById<TextView>(R.id.issues_title).text.toString(),
                )
                val first = area.issuesContainer().getChildAt(0)
                Assert.assertEquals(
                    "Bitcoin ATM",
                    first.findViewById<TextView>(R.id.title).text.toString(),
                )
                Assert.assertEquals(
                    "Missing icon",
                    first.findViewById<TextView>(R.id.subtitle).text.toString(),
                )
                val second = area.issuesContainer().getChildAt(1)
                Assert.assertEquals(
                    "Outdated, needs verification",
                    second.findViewById<TextView>(R.id.subtitle).text.toString(),
                )
            }
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

        withArea { scenario, area ->
            waitUntilOnMain { area.issuesContainer().childCount == 1 }

            scenario.onActivity {
                Assert.assertEquals(
                    "Issues (1)",
                    area.view().findViewById<TextView>(R.id.issues_title).text.toString(),
                )
                Assert.assertEquals(
                    "Not verified",
                    area.issuesContainer().getChildAt(0)
                        .findViewById<TextView>(R.id.subtitle).text.toString(),
                )
            }
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

        withArea { scenario, area ->
            waitUntilOnMain { area.issuesContainer().childCount == 2 }

            scenario.onActivity {
                Assert.assertEquals(
                    "Invalid value for name",
                    area.issuesContainer().getChildAt(0)
                        .findViewById<TextView>(R.id.subtitle).text.toString(),
                )
                Assert.assertEquals(
                    "Unknown issue",
                    area.issuesContainer().getChildAt(1)
                        .findViewById<TextView>(R.id.subtitle).text.toString(),
                )
            }
        }
    }

    @Test
    fun noPlaceIssues_hidesIssuesSection() {
        apiRule.server.dispatcher = areaDispatcher(issuesBody = EMPTY_ISSUES_JSON)
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.view().findViewById<View>(R.id.loading).isVisible }

            Assert.assertFalse(area.view().findViewById<View>(R.id.issues_header).isVisible)
            Assert.assertFalse(area.issuesContainer().isVisible)
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

    private fun AreaFragment.view(): View = requireView()

    private fun AreaFragment.eventsContainer(): ViewGroup =
        view().findViewById(R.id.upcoming_events_container)

    private fun AreaFragment.issuesContainer(): ViewGroup =
        view().findViewById(R.id.issues_container)

    private fun place(osmId: String, icon: String): Place {
        return Place(
            id = 1,
            bundled = false,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = icon,
            name = "Test Place",
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
            osmId = osmId,
        )
    }
}
