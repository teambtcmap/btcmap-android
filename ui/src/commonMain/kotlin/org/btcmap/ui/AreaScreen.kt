package org.btcmap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.btcmap.api.GetEventsItem
import org.btcmap.area.AreaIssues
import org.btcmap.area.AreaPlaceIssue
import org.btcmap.area.descriptionParagraphs
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedName
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds
import org.btcmap.offline.OfflineRegionEstimates
import kotlin.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tags for the area body's sections and rows. */
const val AREA_DESCRIPTION_TAG = "area-description"
const val AREA_HEADER_TAG = "area-header"
const val AREA_READ_MORE_TAG = "area-read-more"
const val AREA_WEBSITE_TAG = "area-website"
const val AREA_BOOSTED_TITLE_TAG = "area-boosted-title"
const val AREA_BOOSTED_CARD_TAG = "area-boosted-card"
const val AREA_EVENTS_TITLE_TAG = "area-events-title"
const val AREA_EVENT_CARD_TAG = "area-event-card"
const val AREA_ISSUES_TITLE_TAG = "area-issues-title"
const val AREA_ISSUE_CARD_TAG = "area-issue-card"
const val AREA_OFFLINE_TAG = "area-offline"
const val AREA_OFFLINE_STATUS_TAG = "area-offline-status"
const val AREA_OFFLINE_PROGRESS_TAG = "area-offline-progress"
const val AREA_OFFLINE_DOWNLOAD_TAG = "area-offline-download"
const val AREA_OFFLINE_DELETE_TAG = "area-offline-delete"

/** How tall the area body's header image is when a host passes one. */
private val AREA_HEADER_HEIGHT = 200.dp

/** The area body's strings and formatters, resolved by the host. */
data class AreaStrings(
    val readMore: String,
    val collapse: String,
    val boostedMerchants: String,
    val events: String,
    val howToHelp: String,
    val offlineMap: String,
    val offlineDownload: String,
    val offlineDownloadAgain: String,
    val offlineDelete: String,
    val cancel: String,
    val boosted: String,
    val boostedUntil: (date: String) -> String,
    val issues: (shown: Long, total: Long) -> String,
    val issueDescription: (code: String) -> String,
    val offlineStatusDownloading: (size: String) -> String,
    val offlineStatusProgress: (percent: Int, size: String) -> String,
    val offlineStatusDownloaded: (size: String, minZoom: Int, maxZoom: Int) -> String,
    val offlineStyleMismatch: String,
    val offlineStatusFailed: (message: String) -> String,
    val offlineDialogDescription: (areaName: String) -> String,
    val offlineDialogStyle: (styleName: String) -> String,
    val offlineDialogMaxZoom: (zoom: Int) -> String,
    val offlineDialogEstimatedSize: (size: String) -> String,
    val offlineDialogTooLarge: (size: String) -> String,
    val offlineDialogEstimateNote: String,
    val formatBytes: (Long) -> String,
)

/** The region and labels the offline download dialog needs. */
data class AreaOfflineDialog(
    val areaName: String,
    val styleName: String,
    val bounds: OfflineBounds,
)

/**
 * The area body: its description and website, the offline map panel and the
 * boosted merchant, upcoming event and place issue sections.
 *
 * The host owns the top bar and scrolls this; the body itself is a plain column
 * so it can sit inside the Views nested scroll view that drives the collapsing
 * toolbar. The three sections come from the shared [org.btcmap.area.AreaSections]
 * loaders.
 */
@Composable
fun AreaScreen(
    description: String?,
    websiteText: String?,
    /** Opens the area's website; null leaves [websiteText] as plain text. */
    onOpenWebsite: (() -> Unit)? = null,
    boostedMerchants: List<Place>,
    events: List<GetEventsItem>,
    issues: AreaIssues?,
    offlineState: OfflineAreaState?,
    offlineStyleMatches: (styleUrl: String) -> Boolean,
    strings: AreaStrings,
    onOpenPlace: (Long) -> Unit,
    onOpenEvent: (GetEventsItem) -> Unit,
    onOpenIssue: (AreaPlaceIssue) -> Unit,
    onJoinUs: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    /**
     * The area's header image, drawn at the top of the body for hosts without
     * the Android collapsing toolbar, which shows it itself. Null hides it, as
     * on Android.
     */
    headerImageUrl: String? = null,
    offlineDialog: AreaOfflineDialog? = null,
    onDismissOfflineDialog: () -> Unit = {},
    onConfirmOfflineDownload: (maxZoom: Int) -> Unit = {},
    /**
     * The boosted marker colour from the user's settings, so the boosted
     * merchants' list icons match their pins on the map.
     */
    boostedMarkerColor: Color = Color(0xFFF7931A),
    modifier: Modifier = Modifier,
) {
    val eventDateFormat = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    val boostDateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // The image is the first child; an 8dp top matches the 8dp spacer
            // under it, so the header's spacing is symmetrical. The bottom keeps
            // the body's 16dp.
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    ) {
        headerImageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AREA_HEADER_HEIGHT)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag(AREA_HEADER_TAG),
            )
            // With a description following, a 16dp gap; with the URL next, 20dp
            // here plus the URL row's own 8dp top padding makes 28dp.
            Spacer(
                modifier = Modifier.height(
                    if (description.isNullOrBlank()) 20.dp else 16.dp,
                ),
            )
        }

        DescriptionSection(description = description, strings = strings)

        websiteText?.let { WebsiteRow(it, onOpenWebsite) }

        OfflinePanel(
            state = offlineState,
            styleMatches = offlineStyleMatches,
            strings = strings,
            onDownload = onDownload,
            onDelete = onDelete,
        )

        if (events.isNotEmpty()) {
            SectionTitle(strings.events, AREA_EVENTS_TITLE_TAG)
            events.forEach { event ->
                AreaCard(
                    glyph = "event",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = event.name,
                    subtitle = event.startsAt.format(eventDateFormat),
                    tag = AREA_EVENT_CARD_TAG,
                    onClick = { onOpenEvent(event) },
                )
            }
        }

        if (boostedMerchants.isNotEmpty()) {
            SectionTitle(strings.boostedMerchants, AREA_BOOSTED_TITLE_TAG)
            boostedMerchants.forEach { place ->
                AreaCard(
                    glyph = place.icon,
                    iconTint = boostedMarkerColor,
                    title = place.getLocalizedName().takeIf { it.isNotBlank() },
                    subtitle = boostSubtitle(place.boostedUntil, boostDateFormat, strings),
                    tag = AREA_BOOSTED_CARD_TAG,
                    onClick = { onOpenPlace(place.id) },
                )
            }
        }

        issues?.takeIf { it.rows.isNotEmpty() }?.let { issues ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
            ) {
                Text(
                    text = strings.issues(issues.rows.size.toLong(), issues.totalIssues),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).testTag(AREA_ISSUES_TITLE_TAG),
                )
                TextButton(onClick = onJoinUs) {
                    Text(strings.howToHelp)
                }
            }

            issues.rows.forEach { issue ->
                val placeIcon = issue.placeIcon?.takeIf { it.isNotBlank() }
                AreaCard(
                    glyph = placeIcon ?: "warning",
                    iconTint = if (placeIcon != null) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    title = issue.elementName.takeIf { it.isNotBlank() },
                    subtitle = strings.issueDescription(issue.issueCode),
                    tag = AREA_ISSUE_CARD_TAG,
                    onClick = { onOpenIssue(issue) },
                )
            }
        }
    }

    offlineDialog?.let { dialog ->
        OfflineMapDialog(
            dialog = dialog,
            strings = strings,
            onDismiss = onDismissOfflineDialog,
            onConfirm = onConfirmOfflineDownload,
        )
    }
}

@Composable
private fun DescriptionSection(description: String?, strings: AreaStrings) {
    if (description.isNullOrBlank()) return

    val paragraphs = remember(description) { descriptionParagraphs(description) }
    if (paragraphs.size <= 1) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(AREA_DESCRIPTION_TAG),
        )
        return
    }

    var expanded by remember(description) { mutableStateOf(false) }
    Text(
        text = if (expanded) paragraphs.joinToString("\n\n") else paragraphs.first(),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(AREA_DESCRIPTION_TAG),
    )
    TextButton(
        onClick = { expanded = !expanded },
        // The default button padding would indent the label past the body text
        // it sits under, so drop it and keep the button aligned to the column.
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.testTag(AREA_READ_MORE_TAG),
    ) {
        Text(if (expanded) strings.collapse else strings.readMore)
    }
}

@Composable
private fun WebsiteRow(text: String, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        MaterialSymbol(
            glyph = "public",
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.secondary
            } else {
                LocalContentColor.current
            },
            modifier = Modifier.padding(start = 16.dp).testTag(AREA_WEBSITE_TAG),
        )
    }
}

@Composable
private fun SectionTitle(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).testTag(tag),
    )
}

@Composable
private fun AreaCard(
    glyph: String,
    iconTint: Color,
    title: String?,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag(tag)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            MaterialSymbol(glyph = glyph, contentDescription = null, tint = iconTint)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun OfflinePanel(
    state: OfflineAreaState?,
    styleMatches: (String) -> Boolean,
    strings: AreaStrings,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    // The panel appears only once a pack exists; before that the toolbar's
    // download action is the only offline affordance.
    val resolved = state?.takeIf { it !is OfflineAreaState.None } ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag(AREA_OFFLINE_TAG),
    ) {
        Text(
            text = strings.offlineMap,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (resolved) {
            is OfflineAreaState.Downloading -> {
                val size = strings.formatBytes(resolved.completedBytes)
                val progress = resolved.progress
                if (progress == null) {
                    Text(
                        text = strings.offlineStatusDownloading(size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).testTag(AREA_OFFLINE_STATUS_TAG),
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(AREA_OFFLINE_PROGRESS_TAG),
                    )
                } else {
                    val percent = (progress * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = strings.offlineStatusProgress(percent, size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).testTag(AREA_OFFLINE_STATUS_TAG),
                    )
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(AREA_OFFLINE_PROGRESS_TAG),
                    )
                }
            }

            is OfflineAreaState.Complete -> {
                val downloaded = strings.offlineStatusDownloaded(
                    strings.formatBytes(resolved.bytes),
                    OfflineRegionEstimates.MIN_ZOOM,
                    resolved.maxZoom,
                )
                Text(
                    text = if (styleMatches(resolved.styleUrl)) {
                        downloaded
                    } else {
                        downloaded + "\n" + strings.offlineStyleMismatch
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).testTag(AREA_OFFLINE_STATUS_TAG),
                )
                OfflineButtons(
                    label = strings.offlineDownloadAgain,
                    showDelete = true,
                    strings = strings,
                    onDownload = onDownload,
                    onDelete = onDelete,
                )
            }

            is OfflineAreaState.Failed -> {
                Text(
                    text = strings.offlineStatusFailed(resolved.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).testTag(AREA_OFFLINE_STATUS_TAG),
                )
                OfflineButtons(
                    label = strings.offlineDownload,
                    showDelete = true,
                    strings = strings,
                    onDownload = onDownload,
                    onDelete = onDelete,
                )
            }

            OfflineAreaState.None -> Unit
        }
    }
}

@Composable
private fun OfflineButtons(
    label: String,
    showDelete: Boolean,
    strings: AreaStrings,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Button(
            onClick = onDownload,
            modifier = Modifier.testTag(AREA_OFFLINE_DOWNLOAD_TAG),
        ) {
            Text(label)
        }
        if (showDelete) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.testTag(AREA_OFFLINE_DELETE_TAG),
            ) {
                Text(strings.offlineDelete)
            }
        }
    }
}

private fun boostSubtitle(
    boostedUntil: Instant?,
    format: DateTimeFormatter,
    strings: AreaStrings,
): String {
    val date = boostedUntil?.format(format) ?: return strings.boosted
    return strings.boostedUntil(date)
}

/**
 * The offline download dialog: it picks the maximum zoom (bounded by
 * [OfflineRegionEstimates]) and shows the estimated size, disabling the
 * download when the region would be too large.
 */
@Composable
private fun OfflineMapDialog(
    dialog: AreaOfflineDialog,
    strings: AreaStrings,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val bounds = dialog.bounds
    val minZoom = OfflineRegionEstimates.MIN_SELECTABLE_MAX_ZOOM
    val maxZoom = OfflineRegionEstimates.maxSelectableZoom(bounds)
    val withinLimit = OfflineRegionEstimates.isWithinLimit(bounds)
    val fixedZoom = maxZoom <= minZoom

    var selectedMaxZoom by remember(bounds) {
        mutableIntStateOf(
            OfflineRegionEstimates.defaultMaxZoom(bounds).coerceIn(minZoom, maxZoom),
        )
    }

    val bytes = OfflineRegionEstimates.estimatedBytes(
        bounds,
        OfflineRegionEstimates.MIN_ZOOM,
        selectedMaxZoom,
    )
    val size = strings.formatBytes(bytes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.offlineMap) },
        text = {
            Column {
                Text(
                    text = strings.offlineDialogDescription(dialog.areaName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.offlineDialogStyle(dialog.styleName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = strings.offlineDialogMaxZoom(selectedMaxZoom),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (!fixedZoom) {
                    Slider(
                        value = selectedMaxZoom.toFloat(),
                        onValueChange = { selectedMaxZoom = it.toInt() },
                        valueRange = minZoom.toFloat()..maxZoom.toFloat(),
                        steps = (maxZoom - minZoom - 1).coerceAtLeast(0),
                    )
                }
                Text(
                    text = if (withinLimit) {
                        strings.offlineDialogEstimatedSize(size)
                    } else {
                        strings.offlineDialogTooLarge(size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = strings.offlineDialogEstimateNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedMaxZoom) },
                enabled = withinLimit,
            ) {
                Text(strings.offlineDownload)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
