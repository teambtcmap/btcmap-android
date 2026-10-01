package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedName
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmUrl
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The place details shown when a marker is selected, the first stage of the
 * `PlaceFragment` bottom sheet: the name, the verification state, the contact
 * details and the main actions. The photos, the preview map and the inline
 * comment list are still to come.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSheet(
    place: Place,
    comments: List<CommentsAdapterItem>,
    strings: PlaceSheetStrings,
    onAction: (PlaceAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PlaceSheetContent(
            place = place,
            comments = comments,
            strings = strings,
            onAction = onAction,
        )
    }
}

@Composable
private fun PlaceSheetContent(
    place: Place,
    comments: List<CommentsAdapterItem>,
    strings: PlaceSheetStrings,
    onAction: (PlaceAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
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
            PlaceOverflowMenu(place = place, strings = strings, onAction = onAction)
        }

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

        place.address?.takeIf { it.isNotBlank() }?.let { InfoRow(glyph = "place", text = it) }
        place.phone?.takeIf { it.isNotBlank() }?.let { InfoRow(glyph = "call", text = it) }
        place.website?.let {
            InfoRow(glyph = "public", text = it.toString().replace("https://", "").trimEnd('/'))
        }
        place.email?.let { InfoRow(glyph = "mail", text = it) }
        place.telegram?.let {
            InfoRow(glyph = "send", text = it.toString().replace("https://t.me/", ""))
        }
        place.line?.let { InfoRow(glyph = "chat", text = it.toString()) }
        place.twitter?.let { InfoRow(glyph = "link", text = it.toString()) }
        place.facebook?.let { InfoRow(glyph = "link", text = it.toString()) }
        place.instagram?.let { InfoRow(glyph = "link", text = it.toString()) }
        place.openingHours?.takeIf { it.isNotBlank() }?.let { InfoRow(glyph = "schedule", text = it) }

        Spacer(modifier = Modifier.width(16.dp))

        ActionRow(
            startLabel = strings.verify,
            onStart = { onAction(PlaceAction.Verify) },
            endLabel = strings.report,
            onEnd = { onAction(PlaceAction.Report) },
        )
        ActionRow(
            startLabel = strings.boost,
            onStart = { onAction(PlaceAction.Boost) },
            endLabel = strings.comments(place.comments ?: 0),
            onEnd = { onAction(PlaceAction.Comments) },
        )

        if (comments.isNotEmpty()) {
            InfoRow(glyph = "comment", text = strings.commentsTitle(comments.size.toLong()))
            comments.forEach { CommentRow(item = it) }
        }

        OutlinedButton(
            onClick = { onAction(PlaceAction.AddComment) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
private fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = label) },
        onClick = onClick,
    )
}

@Composable
private fun InfoRow(
    glyph: String,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionRow(
    startLabel: String,
    onStart: () -> Unit,
    endLabel: String,
    onEnd: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedButton(onClick = onStart, modifier = Modifier.weight(1f)) {
            Text(text = startLabel, maxLines = 1)
        }
        OutlinedButton(onClick = onEnd, modifier = Modifier.weight(1f)) {
            Text(text = endLabel, maxLines = 1)
        }
    }
}

private fun formatVerifiedAt(verifiedAt: ZonedDateTime): String {
    return verifiedAt.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

/** True when the place has never been verified, or not within the last year. */
private fun Place.isOutdated(now: ZonedDateTime = ZonedDateTime.now()): Boolean {
    val verifiedAt = verifiedAt ?: return true
    return verifiedAt.isBefore(now.minusYears(1))
}
