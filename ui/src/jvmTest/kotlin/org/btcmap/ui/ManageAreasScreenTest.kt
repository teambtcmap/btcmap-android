package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.db.table.area.Area
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The manage-areas screen: unverified areas first, then the oldest verification dates. */
class ManageAreasScreenTest {

    private val labels = ManageAreasLabels(
        search = "Search areas",
        clear = "Clear search",
        empty = "No areas found",
        noMatches = "No matching areas",
        failed = "Couldn't load the areas",
        retry = "Retry",
        notVerified = "Not verified",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onlyCommunityAreas_areListed() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, "Community One", type = "community"),
                            area(2, "Some Country", type = "country"),
                            area(3, "Some City", type = "city"),
                        )
                    },
                )
            }
            waitForIdle()

            assertTrue(onAllNodesWithText("Community One").fetchSemanticsNodes().isNotEmpty())
            assertTrue(onAllNodesWithText("Some Country").fetchSemanticsNodes().isEmpty())
            assertTrue(onAllNodesWithText("Some City").fetchSemanticsNodes().isEmpty())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unverifiedAreas_fallBackToAlphabeticalName() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, "Zurich"),
                            area(2, "amsterdam"),
                            area(3, "Berlin"),
                        )
                    },
                )
            }
            waitForIdle()

            val amsterdam = onNodeWithText("amsterdam").fetchSemanticsNode().boundsInRoot
            val berlin = onNodeWithText("Berlin").fetchSemanticsNode().boundsInRoot
            val zurich = onNodeWithText("Zurich").fetchSemanticsNode().boundsInRoot
            assertTrue(amsterdam.top < berlin.top, "amsterdam should come before Berlin")
            assertTrue(berlin.top < zurich.top, "Berlin should come before Zurich")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun areas_areOrderedUnverifiedThenOldestVerified() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, "Newer", verifiedAt = "2026-01-01"),
                            area(2, "Older", verifiedAt = "2024-01-01"),
                            area(3, "Unverified B"),
                            area(4, "Unverified A"),
                        )
                    },
                )
            }
            waitForIdle()

            val unverifiedA = onNodeWithText("Unverified A").fetchSemanticsNode().boundsInRoot
            val unverifiedB = onNodeWithText("Unverified B").fetchSemanticsNode().boundsInRoot
            val older = onNodeWithText("Older").fetchSemanticsNode().boundsInRoot
            val newer = onNodeWithText("Newer").fetchSemanticsNode().boundsInRoot
            assertTrue(unverifiedA.top < unverifiedB.top, "unverified areas come first, by name")
            assertTrue(unverifiedB.top < older.top, "every unverified area precedes a verified one")
            assertTrue(older.top < newer.top, "the oldest verification date comes first")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun verifiedArea_showsItsDate_andUnverifiedWearsTheWarning() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, "Unverified"),
                            area(2, "Verified", verifiedAt = "2025-06-30"),
                        )
                    },
                )
            }
            waitForIdle()

            assertTrue(onAllNodesWithText("2025-06-30").fetchSemanticsNodes().isNotEmpty())
            assertTrue(
                onAllNodesWithContentDescription("Not verified").fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun namelessArea_isLabelledEarthAndSortsAmongTheEs() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, ""),
                            area(2, "Berlin"),
                            area(3, "Zurich"),
                        )
                    },
                )
            }
            waitForIdle()

            val berlin = onNodeWithText("Berlin").fetchSemanticsNode().boundsInRoot
            val earth = onNodeWithText("Earth").fetchSemanticsNode().boundsInRoot
            val zurich = onNodeWithText("Zurich").fetchSemanticsNode().boundsInRoot
            assertTrue(berlin.top < earth.top, "Berlin should come before Earth")
            assertTrue(earth.top < zurich.top, "Earth should come before Zurich")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingARow_opensThatArea() {
        var opened: Area? = null
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = { listOf(area(7, "Berlin")) },
                    onAreaClick = { opened = it },
                )
            }
            waitForIdle()

            onNodeWithText("Berlin").performClick()
        }
        assertEquals(7L, opened?.id)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyCache_showsTheEmptyMessage() {
        runComposeUiTest {
            setContent { ManageAreasScreen(labels = labels, load = { emptyList() }) }
            waitForIdle()

            assertTrue(onAllNodesWithText("No areas found").fetchSemanticsNodes().isNotEmpty())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun search_filtersToTheNamesThatContainTheQuery() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(
                    labels = labels,
                    load = {
                        listOf(
                            area(1, "Amsterdam"),
                            area(2, "Berlin"),
                            area(3, "Bern"),
                        )
                    },
                )
            }
            waitForIdle()

            onNodeWithTag(MANAGE_AREAS_SEARCH_TAG).performTextInput("ber")
            waitForIdle()

            // A case-insensitive substring match: Berlin and Bern stay, Amsterdam goes.
            assertTrue(onAllNodesWithText("Berlin").fetchSemanticsNodes().isNotEmpty())
            assertTrue(onAllNodesWithText("Bern").fetchSemanticsNodes().isNotEmpty())
            assertTrue(onAllNodesWithText("Amsterdam").fetchSemanticsNodes().isEmpty())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun search_withNoMatch_showsTheNoMatchesMessage() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(labels = labels, load = { listOf(area(1, "Berlin")) })
            }
            waitForIdle()

            onNodeWithTag(MANAGE_AREAS_SEARCH_TAG).performTextInput("zzz")
            waitForIdle()

            assertTrue(onAllNodesWithText("No matching areas").fetchSemanticsNodes().isNotEmpty())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadFailure_showsTheRetryAction() {
        runComposeUiTest {
            setContent {
                ManageAreasScreen(labels = labels, load = { error("could not read the cache") })
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Couldn't load the areas").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").assertIsDisplayed()
        }
    }

    private fun area(
        id: Long,
        name: String,
        type: String = "community",
        verifiedAt: String? = null,
    ) = Area(
        id = id,
        name = name,
        type = type,
        urlAlias = "alias",
        icon = null,
        iconWide = null,
        websiteUrl = "https://btcmap.org",
        description = null,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = null,
        verifiedAt = verifiedAt,
    )
}
