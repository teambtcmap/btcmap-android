package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tag so a test can retry a failed load. */
const val MY_EVENTS_RETRY_TAG = "my-events-retry"

/** Test tag prefix for a card's revoke button, keyed by the event id. */
const val MY_EVENTS_REVOKE_TAG_PREFIX = "my-events-revoke-"

/** Test tag prefix for a card's duplicate button, keyed by the event id. */
const val MY_EVENTS_DUPLICATE_TAG_PREFIX = "my-events-duplicate-"

/** The height of the map banner at the top of each event card. */
private val EVENT_CARD_MAP_HEIGHT = 140.dp

/** A submitted event's review state, as the API reports it. */
enum class MyEventStatus {
    Pending,
    Live,
    Rejected;

    companion object {
        /**
         * Maps the API's status string. Anything unrecognised reads as pending,
         * the only state a fresh submission can be in.
         */
        fun fromValue(value: String): MyEventStatus = when (value) {
            "live" -> Live
            "rejected" -> Rejected
            else -> Pending
        }
    }
}

/**
 * One of the signed-in user's submitted events: enough to render it and to seed
 * a duplicate. [startsAtLocal] and [endsAtLocal] are the event's own wall-clock
 * times (no offset), so repeating it keeps the same local hour.
 */
data class MyEventUi(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    val status: MyEventStatus,
    val startsAtLocal: String,
    val endsAtLocal: String?,
)

/** The my-events screen's strings, so it stays resource-free. */
data class MyEventsLabels(
    val empty: String,
    val failed: String,
    val retry: String,
    val duplicate: String,
    val revoke: String,
    val revokeFailed: String,
    val statusPending: String,
    val statusLive: String,
    val statusRejected: String,
    /** Formats a same-day event's date and its start and end times. */
    val dateRange: (date: String, start: String, end: String) -> String,
)

/**
 * The events the signed-in user has submitted, newest first, each a card with a
 * full-width map banner, its date and review status, and duplicate/revoke
 * actions. The list is loaded through [load] and revoked through [revoke], so the
 * host owns the API calls and the screen stays testable; [onDuplicate] hands an
 * event to the host to open a pre-filled add-event screen.
 *
 * [map] renders the per-event banner; the host supplies it so a test can render
 * the list without a GPU. The screen carries no title of its own: the host shows
 * "My events" in its own title bar, and its back affordance leaves this screen.
 */
@Composable
fun MyEventsScreen(
    labels: MyEventsLabels,
    load: suspend () -> List<MyEventUi>,
    revoke: suspend (MyEventUi) -> Unit,
    onDuplicate: (MyEventUi) -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable (event: MyEventUi, modifier: Modifier) -> Unit = { _, _ -> },
) {
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<MyEventUi>>(emptyList()) }
    val revoking = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loadError = null
        try {
            events = orderMyEventsByDate(load(), Clock.System.now())
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            loadError = t.message ?: t.toString()
        } finally {
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    val error = loadError
    when {
        !loaded -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> ErrorState(
            message = labels.failed,
            retry = labels.retry,
            onRetry = { scope.launch { reload() } },
            modifier = modifier,
        )

        events.isEmpty() -> EmptyState(message = labels.empty, modifier = modifier)

        else -> Column(modifier = modifier.fillMaxSize()) {
            actionError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(events, key = { it.id }) { event ->
                    MyEventCard(
                        event = event,
                        labels = labels,
                        revoking = event.id in revoking,
                        map = map,
                        onDuplicate = { onDuplicate(event) },
                        onRevoke = {
                            actionError = null
                            scope.launch {
                                revoking.add(event.id)
                                try {
                                    revoke(event)
                                    // Revocation soft-deletes the event, so it
                                    // is gone from /me/events; drop the card
                                    // rather than refetch the list.
                                    events = events.filterNot { it.id == event.id }
                                } catch (t: Throwable) {
                                    t.rethrowIfCancellation()
                                    actionError = labels.revokeFailed
                                } finally {
                                    revoking.remove(event.id)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MyEventCard(
    event: MyEventUi,
    labels: MyEventsLabels,
    revoking: Boolean,
    onDuplicate: () -> Unit,
    onRevoke: () -> Unit,
    map: @Composable (event: MyEventUi, modifier: Modifier) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Full-width map banner; the card clips it to its rounded corners.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EVENT_CARD_MAP_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                map(event, Modifier.fillMaxSize())
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = eventTimeText(event, labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                // Footer: the review state on the start, the actions on the end,
                // so the card reads title -> date -> status/actions.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    StatusPill(status = event.status, labels = labels)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDuplicate,
                            modifier = Modifier.testTag(MY_EVENTS_DUPLICATE_TAG_PREFIX + event.id),
                        ) {
                            Text(labels.duplicate)
                        }
                        if (event.status == MyEventStatus.Pending) {
                            TextButton(
                                onClick = onRevoke,
                                enabled = !revoking,
                                modifier = Modifier.testTag(MY_EVENTS_REVOKE_TAG_PREFIX + event.id),
                            ) {
                                if (revoking) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Text(labels.revoke)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun eventTimeText(event: MyEventUi, labels: MyEventsLabels): String {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }

    val start = event.startsAt
    val end = event.endsAt
    return when {
        end == null -> start.format(dateTimeFormatter)

        start.toLocalDate() == end.toLocalDate() -> labels.dateRange(
            start.format(dateFormatter),
            start.format(timeFormatter),
            end.format(timeFormatter),
        )

        else -> start.format(dateTimeFormatter) + " - " + end.format(dateTimeFormatter)
    }
}

/** The review state as a tonal pill, the M3 way to show a non-interactive label. */
@Composable
private fun StatusPill(status: MyEventStatus, labels: MyEventsLabels, modifier: Modifier = Modifier) {
    val container = when (status) {
        MyEventStatus.Pending -> MaterialTheme.colorScheme.tertiaryContainer
        MyEventStatus.Live -> MaterialTheme.colorScheme.primaryContainer
        MyEventStatus.Rejected -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (status) {
        MyEventStatus.Pending -> MaterialTheme.colorScheme.onTertiaryContainer
        MyEventStatus.Live -> MaterialTheme.colorScheme.onPrimaryContainer
        MyEventStatus.Rejected -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier,
    ) {
        Text(
            text = statusLabel(status, labels),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MaterialSymbol(
                glyph = "event_busy",
                contentDescription = null,
                size = 48.sp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    retry: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(MY_EVENTS_RETRY_TAG),
            ) {
                Text(retry)
            }
        }
    }
}

private fun statusLabel(status: MyEventStatus, labels: MyEventsLabels): String = when (status) {
    MyEventStatus.Pending -> labels.statusPending
    MyEventStatus.Live -> labels.statusLive
    MyEventStatus.Rejected -> labels.statusRejected
}

/**
 * Orders the user's saved events for display: the next upcoming events first
 * (soonest first), then past events (most recent first). That puts the event
 * closest to today at the top, rather than the newest submission.
 *
 * [now] is a parameter so the ordering is deterministic in tests.
 */
internal fun orderMyEventsByDate(events: List<MyEventUi>, now: Instant): List<MyEventUi> =
    events.sortedWith { a, b ->
        val aUpcoming = a.startsAt > now
        val bUpcoming = b.startsAt > now
        when {
            aUpcoming != bUpcoming -> if (aUpcoming) -1 else 1
            aUpcoming -> a.startsAt.compareTo(b.startsAt)
            else -> b.startsAt.compareTo(a.startsAt)
        }
    }
