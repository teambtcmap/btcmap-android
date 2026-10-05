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
 * The my-events list: the fetched rows render with their status, an empty result
 * shows the empty state, a failed load can be retried, and a pending event can be
 * revoked.
 */
class MyEventsScreenTest {

    private val pending = MyEventUi(
        id = 1L,
        lat = 7.8,
        lon = 98.3,
        name = "Pending Meetup",
        website = "https://example.com",
        startsAt = Instant.parse("2026-11-01T18:00:00Z"),
        endsAt = null,
        status = MyEventStatus.Pending,
        startsAtLocal = "2026-11-01T18:00:00",
        endsAtLocal = null,
    )
    private val live = MyEventUi(
        id = 2L,
        lat = 7.9,
        lon = 98.4,
        name = "Live Meetup",
        website = "",
        startsAt = Instant.parse("2026-09-01T18:00:00Z"),
        endsAt = Instant.parse("2026-09-01T20:00:00Z"),
        status = MyEventStatus.Live,
        startsAtLocal = "2026-09-01T18:00:00",
        endsAtLocal = "2026-09-01T20:00:00",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersEventsWithStatuses() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = { listOf(pending, live) },
                        revoke = {},
                        onDuplicate = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Pending Meetup").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Pending Meetup").assertIsDisplayed()
            onNodeWithText("Pending review").assertIsDisplayed()
            onNodeWithText("Live Meetup").assertIsDisplayed()
            onNodeWithText("Live").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyList_showsTheEmptyState() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = { emptyList() },
                        revoke = {},
                        onDuplicate = {},
                    )
                }
            }
            onNodeWithText("You haven't submitted any events yet.").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedLoad_retriesIntoTheList() {
        var attempts = 0
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = {
                            attempts++
                            if (attempts == 1) throw RuntimeException("boom") else listOf(live)
                        },
                        revoke = {},
                        onDuplicate = {},
                    )
                }
            }
            onNodeWithTag(MY_EVENTS_RETRY_TAG).assertIsDisplayed()
            onNodeWithTag(MY_EVENTS_RETRY_TAG).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Live Meetup").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Live Meetup").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun revokePending_callsBackAndRemovesTheCard() {
        val revoked = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = { listOf(pending, live) },
                        revoke = { revoked += it.id },
                        onDuplicate = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(MY_EVENTS_REVOKE_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(MY_EVENTS_REVOKE_TAG_PREFIX + pending.id).performClick()
            // Revocation soft-deletes the event, so its card is dropped.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Pending Meetup").fetchSemanticsNodes().isEmpty()
            }
            onNodeWithText("Live Meetup").assertIsDisplayed()
        }
        assertEquals(listOf(pending.id), revoked)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun revokeFailure_showsTheError() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = { listOf(pending) },
                        revoke = { throw RuntimeException("403") },
                        onDuplicate = {},
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(MY_EVENTS_REVOKE_TAG_PREFIX + pending.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(MY_EVENTS_REVOKE_TAG_PREFIX + pending.id).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Couldn't revoke the event").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Couldn't revoke the event").assertIsDisplayed()
            // The row keeps its pending state when the revoke failed.
            onNodeWithText("Pending review").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun duplicate_callsBackWithTheEvent() {
        val duplicated = mutableListOf<MyEventUi>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    MyEventsScreen(
                        labels = TEST_MY_EVENTS_LABELS,
                        load = { listOf(live) },
                        revoke = {},
                        onDuplicate = { duplicated += it },
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(MY_EVENTS_DUPLICATE_TAG_PREFIX + live.id)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithTag(MY_EVENTS_DUPLICATE_TAG_PREFIX + live.id).performClick()
        }
        assertEquals(listOf(live), duplicated)
    }

    @Test
    fun orderMyEventsByDate_putsTheClosestFirst() {
        val now = Instant.parse("2026-10-05T12:00:00Z")
        fun event(id: Long, start: String) = MyEventUi(
            id = id,
            lat = 0.0,
            lon = 0.0,
            name = "Event $id",
            website = "",
            startsAt = Instant.parse(start),
            endsAt = null,
            status = MyEventStatus.Pending,
            startsAtLocal = start,
            endsAtLocal = null,
        )

        val ordered = orderMyEventsByDate(
            listOf(
                event(1L, "2026-09-01T18:00:00Z"), // oldest past
                event(2L, "2026-12-01T18:00:00Z"), // furthest future
                event(3L, "2026-10-07T18:00:00Z"), // soonest upcoming
                event(4L, "2026-09-20T18:00:00Z"), // most recent past
            ),
            now,
        )

        // Upcoming soonest first, then past most recent first.
        assertEquals(listOf(3L, 2L, 4L, 1L), ordered.map { it.id })
    }
}
