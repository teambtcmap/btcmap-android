package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection

/**
 * The shared stats cards: an entry with details is a toggle, revealing its
 * details on one tap and hiding them on the next.
 */
class StatsScreenTest {

    private val section = StatsSection(
        key = "tables",
        title = "Tables",
        entries = listOf(
            StatsEntry(
                label = "place",
                value = "10/8/2",
                details = listOf(
                    StatsEntry("Rows", "10"),
                    StatsEntry("Visible", "8"),
                ),
            ),
        ),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingAnEntryTogglesItsDetails() {
        runComposeUiTest {
            setContent { StatsScreen(sections = listOf(section)) }

            onNodeWithText("place").assertIsDisplayed()
            onNodeWithTag(statsEntryTag("tables", "place")).assertHasClickAction()
            onNodeWithTag(statsDetailTag("tables", "place", "Rows")).assertDoesNotExist()

            onNodeWithTag(statsEntryTag("tables", "place")).performClick()
            onNodeWithTag(statsDetailTag("tables", "place", "Rows")).assertIsDisplayed()
            onNodeWithTag(statsDetailTag("tables", "place", "Visible")).assertIsDisplayed()

            // A second tap collapses it again.
            onNodeWithTag(statsEntryTag("tables", "place")).performClick()
            onNodeWithTag(statsDetailTag("tables", "place", "Rows")).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun anEntryWithoutDetailsIsNotTappable() {
        val plain = StatsSection(
            key = "database",
            title = "Database",
            entries = listOf(StatsEntry("Version", "3")),
        )

        runComposeUiTest {
            setContent { StatsScreen(sections = listOf(plain)) }

            onNodeWithText("Version").assertIsDisplayed()
            // A plain entry has no reveal affordance to click.
            onNodeWithTag(statsEntryTag("database", "Version")).assertHasNoClickAction()
        }
    }
}
