package org.btcmap.ui

import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimePeriod
import kotlin.time.Clock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedName
import org.btcmap.openinghours.toOpeningHours
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmUrl
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Test tags for the place body's action buttons. */
const val PLACE_VERIFY_TAG = "place-verify"
const val PLACE_REPORT_TAG = "place-report"
const val PLACE_BOOST_TAG = "place-boost"
const val PLACE_WATCH_TAG = "place-watch"
const val PLACE_ADD_COMMENT_TAG = "place-add-comment"
const val PLACE_ADD_PHOTO_TAG = "place-add-photo"

/**
 * The place details shown when a marker is selected, the map's place sheet: the
 * name, the verification state, the contact details and the main actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSheet(
    place: Place,
    comments: List<CommentsAdapterItem>,
    photos: List<String>,
    bookmarked: Boolean,
    strings: PlaceSheetStrings,
    onAction: (PlaceAction) -> Unit,
    onDismiss: () -> Unit,
    previewMap: (@Composable () -> Unit)? = null,
) {
    // Open at the half-expanded height the Views sheet used, so the map stays
    // visible behind it; the user can drag it up to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PlaceDetails(
            place = place,
            comments = comments,
            photos = photos,
            bookmarked = bookmarked,
            strings = strings,
            onAction = onAction,
            previewMap = previewMap,
            showHeader = true,
        )
    }
}

/**
 * The place's scrollable body: its preview map, verification state, contact
 * details, photos, comments and actions. Shared by the map's [PlaceSheet] and
 * the standalone place screen, which supplies its own top bar and therefore
 * hides [showHeader]'s name row and overflow menu.
 */
@Composable
fun PlaceDetails(
    place: Place,
    comments: List<CommentsAdapterItem>,
    photos: List<String>,
    bookmarked: Boolean,
    strings: PlaceSheetStrings,
    onAction: (PlaceAction) -> Unit,
    previewMap: (@Composable () -> Unit)? = null,
    showHeader: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            ) {
                Text(
                    text = place.getLocalizedName(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                PlaceOverflowMenu(
                    place = place,
                    strings = strings,
                    onAction = onAction,
                )
            }
        }

        previewMap?.invoke()

        // The photos sit above the verification state and the contact details.
        var viewerIndex by remember { mutableStateOf<Int?>(null) }

        if (photos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                itemsIndexed(photos) { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewerIndex = index },
                    )
                }
                // With photos, the add affordance is a trailing tile in the
                // strip rather than a separate full-width button.
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onAction(PlaceAction.AddPhoto) }
                            .testTag(PLACE_ADD_PHOTO_TAG),
                    ) {
                        MaterialSymbol(
                            glyph = "add",
                            contentDescription = strings.addPhoto,
                        )
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = { onAction(PlaceAction.AddPhoto) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag(PLACE_ADD_PHOTO_TAG),
            ) {
                Text(text = strings.addPhoto)
            }
        }

        viewerIndex?.let { index ->
            PlacePhotoViewer(
                photos = photos,
                initialIndex = index,
                onDismiss = { viewerIndex = null },
            )
        }

        // A little breathing room between the photo carousel and the actions.
        // The standalone add-photo button already carries its own padding.
        if (photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // The actions sit directly below the photo carousel.
        ActionRow(
            startLabel = strings.verify,
            onStart = { onAction(PlaceAction.Verify) },
            startGlyph = "verified",
            startTag = PLACE_VERIFY_TAG,
            endLabel = strings.report,
            onEnd = { onAction(PlaceAction.Report) },
            endGlyph = "warning",
            endTag = PLACE_REPORT_TAG,
        )
        ActionRow(
            startLabel = strings.boost,
            onStart = { onAction(PlaceAction.Boost) },
            startGlyph = "rocket_launch",
            startTag = PLACE_BOOST_TAG,
            endLabel = if (bookmarked) strings.unwatch else strings.watch,
            onEnd = { onAction(PlaceAction.ToggleBookmark) },
            endGlyph = if (bookmarked) "bookmark_remove" else "bookmark_add",
            endTag = PLACE_WATCH_TAG,
            bottomPadding = 0.dp,
        )

        place.requiredAppUrl?.let { requiredAppUrl ->
            InfoRow(
                glyph = "warning",
                text = strings.companionWarning(requiredAppUrl.toString().trimEnd('/')),
                tint = MaterialTheme.colorScheme.error,
            )
        }

        val outdated = place.isOutdated()
        val verifiedAt = place.verifiedAt
        var warning by remember { mutableStateOf<String?>(null) }

        if (verifiedAt == null) {
            InfoRow(
                glyph = "verified",
                text = strings.notVerified,
                tint = MaterialTheme.colorScheme.error,
                onClick = { warning = strings.verificationWarningNotVerified },
            )
        } else {
            InfoRow(
                glyph = "verified",
                text = formatVerifiedAt(verifiedAt),
                tint = if (outdated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = if (outdated) {
                    { warning = strings.verificationWarningOutdated }
                } else {
                    null
                },
            )
        }

        warning?.let { message ->
            AlertDialog(
                onDismissRequest = { warning = null },
                title = { Text(text = strings.verificationWarningTitle) },
                text = { Text(text = message) },
                confirmButton = {
                    TextButton(onClick = { warning = null }) {
                        Text(text = strings.ok)
                    }
                },
            )
        }

        place.address?.takeIf { it.isNotBlank() }?.let {
            InfoRow(glyph = "place", text = it, onClick = { onAction(PlaceAction.Directions) })
        }
        place.phone?.takeIf { it.isNotBlank() }?.let {
            InfoRow(glyph = "call", text = it, onClick = { onAction(PlaceAction.Phone) })
        }
        place.website?.let {
            InfoRow(
                glyph = "public",
                text = it.toString().replace("https://", "").trimEnd('/'),
                onClick = { onAction(PlaceAction.Website) },
            )
        }
        place.email?.let {
            InfoRow(glyph = "mail", text = it, onClick = { onAction(PlaceAction.Email) })
        }
        place.telegram?.let {
            InfoRow(
                glyph = "send",
                text = it.toString().replace("https://t.me/", ""),
                onClick = { onAction(PlaceAction.Telegram) },
            )
        }
        place.line?.let {
            InfoRow(glyph = "chat", text = it.toString(), onClick = { onAction(PlaceAction.Line) })
        }
        place.twitter?.let {
            InfoRow(glyph = "link", text = it.toString(), onClick = { onAction(PlaceAction.Twitter) })
        }
        place.facebook?.let {
            InfoRow(
                glyph = "link",
                text = it.toString(),
                onClick = { onAction(PlaceAction.Facebook) },
            )
        }
        place.instagram?.let {
            InfoRow(
                glyph = "link",
                text = it.toString(),
                onClick = { onAction(PlaceAction.Instagram) },
            )
        }
        place.openingHours?.takeIf { it.isNotBlank() }?.let {
            OpeningHoursRow(hours = it, strings = strings)
        }

        if (comments.isNotEmpty()) {
            InfoRow(glyph = "comment", text = strings.commentsTitle(comments.size.toLong()))
            comments.forEach { CommentRow(item = it) }
        }

        OutlinedButton(
            onClick = { onAction(PlaceAction.AddComment) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag(PLACE_ADD_COMMENT_TAG),
        ) {
            Text(text = strings.addComment)
        }
    }
}

@Composable
private fun PlaceOverflowMenu(
    place: Place,
    strings: PlaceSheetStrings,
    onAction: (PlaceAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val osmUrl = place.osmUrl()
    val osmEditUrl = place.osmEditUrl()

    IconButton(onClick = { expanded = true }) {
        MaterialSymbol(glyph = "more_vert", contentDescription = null)
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MenuItem(strings.directions) { expanded = false; onAction(PlaceAction.Directions) }
        MenuItem(strings.share) { expanded = false; onAction(PlaceAction.Share) }
        MenuItem(strings.viewOnBtcmap) { expanded = false; onAction(PlaceAction.ViewOnBtcmap) }
        if (osmUrl != null) {
            MenuItem(strings.viewOnOsm) { expanded = false; onAction(PlaceAction.ViewOnOsm) }
        }
        if (osmEditUrl != null) {
            MenuItem(strings.editOnOsm) { expanded = false; onAction(PlaceAction.EditOnOsm) }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    glyph: String? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = label) },
        leadingIcon = glyph?.let { { MaterialSymbol(glyph = it, contentDescription = null) } },
        onClick = onClick,
    )
}

@Composable
private fun OpeningHoursRow(hours: String, strings: PlaceSheetStrings) {
    val text = remember(hours, strings) {
        openingHoursText(
            raw = hours,
            closedLabel = strings.openingHoursClosed,
            aroundTheClockLabel = strings.openingHoursOpen24_7,
        )
    }
    InfoRow(glyph = "schedule", text = text, maxLines = Int.MAX_VALUE)
}

/**
 * The display text for a place's `opening_hours`: the parsed week, one line per
 * weekday with today's line underlined, or the raw OpenStreetMap value when the
 * week cannot be parsed faithfully. A week that never closes or opens collapses
 * to a single label and has no day to underline.
 */
internal fun openingHoursText(
    raw: String,
    closedLabel: String,
    aroundTheClockLabel: String,
    today: DayOfWeek = LocalDate.now().dayOfWeek,
    locale: Locale = Locale.getDefault(),
): AnnotatedString {
    val hours = raw.toOpeningHours() ?: return AnnotatedString(raw)

    val display = hours.toDisplayString(
        closedLabel = closedLabel,
        aroundTheClockLabel = aroundTheClockLabel,
        locale = locale,
    )
    val lineIndex = hours.todayLineIndex(today) ?: return AnnotatedString(display)

    val lines = display.split('\n')
    val start = lines.take(lineIndex).sumOf { it.length + 1 }
    val end = start + lines[lineIndex].length

    return buildAnnotatedString {
        append(display)
        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
    }
}

@Composable
private fun InfoRow(
    glyph: String,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) = InfoRow(glyph = glyph, text = AnnotatedString(text), tint = tint, onClick = onClick)

@Composable
private fun InfoRow(
    glyph: String,
    text: AnnotatedString,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    maxLines: Int = 2,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        MaterialSymbol(glyph = glyph, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionRow(
    startLabel: String,
    onStart: () -> Unit,
    startGlyph: String? = null,
    startTag: String? = null,
    endLabel: String,
    onEnd: () -> Unit,
    endGlyph: String? = null,
    endTag: String? = null,
    bottomPadding: Dp = 4.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
    ) {
        OutlinedButton(
            onClick = onStart,
            modifier = Modifier
                .weight(1f)
                .then(if (startTag != null) Modifier.testTag(startTag) else Modifier),
        ) {
            ButtonLabel(label = startLabel, glyph = startGlyph)
        }
        OutlinedButton(
            onClick = onEnd,
            modifier = Modifier
                .weight(1f)
                .then(if (endTag != null) Modifier.testTag(endTag) else Modifier),
        ) {
            ButtonLabel(label = endLabel, glyph = endGlyph)
        }
    }
}

/** A button's optional leading icon followed by its label. */
@Composable
private fun ButtonLabel(label: String, glyph: String?) {
    if (glyph != null) {
        MaterialSymbol(glyph = glyph, contentDescription = null, size = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = label, maxLines = 1)
}

private fun formatVerifiedAt(verifiedAt: Instant): String {
    return verifiedAt.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

/** True when the place has never been verified, or not within the last year. */
private fun Place.isOutdated(now: Instant = Clock.System.now()): Boolean {
    val verifiedAt = verifiedAt ?: return true
    return verifiedAt < now.minus(DateTimePeriod(years = 1), TimeZone.UTC)
}
