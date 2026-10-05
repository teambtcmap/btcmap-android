package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The event review queue: pending events render with approve/reject, a resolved
 * event drops out of the list, and a failed action keeps it with an error.
 */
class EventReviewScreenTest {

    private val pending = PendingEventUi(
        id = 1L,
        lat = 7.8,
        lon = 98.3,
        name = "Pending Meetup",
        website = "https://example.com/pending",
        startsAt = Instant.parse("2026-11-01T18:00:00Z"),
        endsAt = null,
    )
    private val other = PendingEventUi(
        id = 2L,
        lat = 7.9,
        lon = 98.4,
        name = "Another Meetup",
        website = "",
        startsAt = Instant.parse("2026-12-01T18:00:00Z"),
        endsAt = null,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun approve_removesTheCardAndCallsBack() {
        val approved = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                EventReviewScreen(
                    labels = TEST_EVENT_REVIEW_LABELS,
                    load = { listOf(pending, other) },
                    approve = { approved += it.id },
                    reject = {},
                    onBack = {},
                    title = "Review events",
                    onOpenUrl = {},
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(EVENT_REVIEW_APPROVE_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(EVENT_REVIEW_APPROVE_TAG_PREFIX + pending.id).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Pending Meetup").fetchSemanticsNodes().isEmpty()
            }
            onNodeWithText("Another Meetup").assertIsDisplayed()
        }
        assertEquals(listOf(pending.id), approved)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reject_removesTheCardAndCallsBack() {
        val rejected = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                EventReviewScreen(
                    labels = TEST_EVENT_REVIEW_LABELS,
                    load = { listOf(pending, other) },
                    approve = {},
                    reject = { rejected += it.id },
                    onBack = {},
                    title = "Review events",
                    onOpenUrl = {},
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(EVENT_REVIEW_REJECT_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(EVENT_REVIEW_REJECT_TAG_PREFIX + pending.id).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Pending Meetup").fetchSemanticsNodes().isEmpty()
            }
            onNodeWithText("Another Meetup").assertIsDisplayed()
        }
        assertEquals(listOf(pending.id), rejected)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyQueue_showsTheEmptyState() {
        runComposeUiTest {
            setContent {
                EventReviewScreen(
                    labels = TEST_EVENT_REVIEW_LABELS,
                    load = { emptyList() },
                    approve = {},
                    reject = {},
                    onBack = {},
                    title = "Review events",
                    onOpenUrl = {},
                )
            }
            onNodeWithText("No events are waiting for review").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedAction_keepsTheCardAndShowsAnError() {
        runComposeUiTest {
            setContent {
                EventReviewScreen(
                    labels = TEST_EVENT_REVIEW_LABELS,
                    load = { listOf(pending) },
                    approve = { throw RuntimeException("403") },
                    reject = {},
                    onBack = {},
                    title = "Review events",
                    onOpenUrl = {},
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(EVENT_REVIEW_APPROVE_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(EVENT_REVIEW_APPROVE_TAG_PREFIX + pending.id).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Couldn't update the event").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Pending Meetup").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun websiteLink_opensTheUrl() {
        val opened = mutableListOf<String>()
        runComposeUiTest {
            setContent {
                EventReviewScreen(
                    labels = TEST_EVENT_REVIEW_LABELS,
                    load = { listOf(pending) },
                    approve = {},
                    reject = {},
                    onBack = {},
                    title = "Review events",
                    onOpenUrl = { opened += it },
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(EVENT_REVIEW_WEBSITE_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(EVENT_REVIEW_WEBSITE_TAG_PREFIX + pending.id).performClick()
        }
        assertEquals(listOf("https://example.com/pending"), opened)
    }
}
