package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tag so a test can retry a failed load. */
const val EVENT_REVIEW_RETRY_TAG = "event-review-retry"

/** Test tag prefix for a card's approve button, keyed by the event id. */
const val EVENT_REVIEW_APPROVE_TAG_PREFIX = "event-review-approve-"

/** Test tag prefix for a card's reject button, keyed by the event id. */
const val EVENT_REVIEW_REJECT_TAG_PREFIX = "event-review-reject-"

/** Test tag prefix for a card's website link, keyed by the event id. */
const val EVENT_REVIEW_WEBSITE_TAG_PREFIX = "event-review-website-"

/** The height of the map banner at the top of each review card. */
private val REVIEW_CARD_MAP_HEIGHT = 140.dp

/** An event awaiting review. */
data class PendingEventUi(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: String,
    val startsAt: Instant,
    val endsAt: Instant?,
)

/** The event review screen's strings, so it stays resource-free. */
data class EventReviewLabels(
    val back: String,
    val empty: String,
    val failed: String,
    val retry: String,
    val approve: String,
    val reject: String,
    val actionFailed: String,
    /** Formats a same-day event's date and its start and end times. */
    val dateRange: (date: String, start: String, end: String) -> String,
)

/**
 * The moderator queue: the events awaiting review, each a card with a
 * full-width map preview, its date, and approve/reject actions. The list is
 * loaded through [load] and resolved through [approve]/[reject], so the host
 * owns the API calls and the screen stays testable; a resolved event drops out
 * of the list without a refetch.
 *
 * [map] renders the per-event banner; the host supplies it so a test can render
 * the list without a GPU. The screen draws its own top bar and carries the title
 * itself, so a host can show it as a full screen.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventReviewScreen(
    labels: EventReviewLabels,
    load: suspend () -> List<PendingEventUi>,
    approve: suspend (PendingEventUi) -> Unit,
    reject: suspend (PendingEventUi) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    iconFont: FontFamily? = null,
    map: @Composable (event: PendingEventUi, modifier: Modifier) -> Unit = { _, _ -> },
) {
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PendingEventUi>>(emptyList()) }
    val acting = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // A failed approve/reject surfaces as a snackbar; the queue stays put.
    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            actionError = null
        }
    }

    suspend fun reload() {
        loadError = null
        try {
            events = load()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            loadError = t.message ?: t.toString()
        } finally {
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun resolve(event: PendingEventUi, action: suspend (PendingEventUi) -> Unit) {
        actionError = null
        scope.launch {
            acting.add(event.id)
            try {
                action(event)
                // An approved event goes live and a rejected one is closed, so
                // either way it leaves the queue.
                events = events.filterNot { it.id == event.id }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                actionError = labels.actionFailed
            } finally {
                acting.remove(event.id)
            }
        }
    }

    AppTheme(iconFont = iconFont) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            MaterialSymbol(glyph = "arrow_back", contentDescription = labels.back)
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                val error = loadError
                when {
                    !loaded -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    error != null -> ErrorState(
                        message = labels.failed,
                        retry = labels.retry,
                        onRetry = { scope.launch { reload() } },
                        modifier = Modifier.fillMaxSize(),
                    )

                    events.isEmpty() -> EmptyState(
                        message = labels.empty,
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(events, key = { it.id }) { event ->
                            PendingEventCard(
                                event = event,
                                labels = labels,
                                busy = event.id in acting,
                                map = map,
                                onOpenUrl = onOpenUrl,
                                onApprove = { resolve(event, approve) },
                                onReject = { resolve(event, reject) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingEventCard(
    event: PendingEventUi,
    labels: EventReviewLabels,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onOpenUrl: (String) -> Unit,
    map: @Composable (event: PendingEventUi, modifier: Modifier) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(REVIEW_CARD_MAP_HEIGHT)
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
                if (event.website.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenUrl(event.website) }
                            .padding(vertical = 4.dp)
                            .testTag(EVENT_REVIEW_WEBSITE_TAG_PREFIX + event.id),
                    ) {
                        MaterialSymbol(
                            glyph = "open_in_new",
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = event.website,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    TextButton(
                        onClick = onReject,
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.testTag(EVENT_REVIEW_REJECT_TAG_PREFIX + event.id),
                    ) {
                        Text(labels.reject)
                    }
                    Button(
                        onClick = onApprove,
                        enabled = !busy,
                        modifier = Modifier.testTag(EVENT_REVIEW_APPROVE_TAG_PREFIX + event.id),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(labels.approve)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun eventTimeText(event: PendingEventUi, labels: EventReviewLabels): String {
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

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MaterialSymbol(
                glyph = "how_to_reg",
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
                    .testTag(EVENT_REVIEW_RETRY_TAG),
            ) {
                Text(retry)
            }
        }
    }
}
