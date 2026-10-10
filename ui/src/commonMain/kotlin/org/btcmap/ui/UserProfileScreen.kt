package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The already-resolved strings [UserProfileScreen] renders. */
data class UserProfileLabels(
    val username: String,
    val password: String,
    val savedPlaces: String,
    val savedAreas: String,
    val logOut: String,
    val editUsername: String,
    val editPassword: String,
    val uploadedImages: String,
    val myEvents: String,
    val myNotes: String,
)

data class UserProfileUiState(
    val username: String,
    val password: String,
    val labels: UserProfileLabels,
)

@Composable
fun UserProfileScreen(
    state: UserProfileUiState,
    onEditUsername: () -> Unit,
    onEditPassword: () -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onOpenSavedAreas: () -> Unit,
    onOpenUploadedImages: () -> Unit,
    onOpenMyEvents: () -> Unit,
    onOpenMyNotes: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionLabel(state.labels.username, glyph = "person")
        EditRow(state.username, state.labels.editUsername, onEditUsername)

        SectionLabel(state.labels.password, glyph = "lock")
        EditRow(state.password, state.labels.editPassword, onEditPassword)

        NavButton(
            glyph = "bookmark",
            text = state.labels.savedPlaces,
            topPadding = 24.dp,
            onClick = onOpenSavedPlaces,
        )
        NavButton(
            glyph = "public",
            text = state.labels.savedAreas,
            topPadding = 8.dp,
            onClick = onOpenSavedAreas,
        )
        NavButton(
            glyph = "event",
            text = state.labels.myEvents,
            topPadding = 8.dp,
            onClick = onOpenMyEvents,
        )
        NavButton(
            glyph = "notes",
            text = state.labels.myNotes,
            topPadding = 8.dp,
            onClick = onOpenMyNotes,
        )
        NavButton(
            glyph = "photo_library",
            text = state.labels.uploadedImages,
            topPadding = 8.dp,
            onClick = onOpenUploadedImages,
        )

        Button(
            onClick = onLogOut,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            MaterialSymbol(glyph = "logout", contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(state.labels.logOut)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String, glyph: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    ) {
        MaterialSymbol(
            glyph = glyph,
            contentDescription = null,
            size = 18.sp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavButton(glyph: String, text: String, topPadding: Dp, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = topPadding)
            .fillMaxWidth(),
    ) {
        MaterialSymbol(glyph = glyph, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun EditRow(value: String, editDescription: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconGlyph("edit", editDescription, onEdit)
    }
}

/** A Material Symbols icon button; the ligature name is not announced. */
@Composable
private fun IconGlyph(glyph: String, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
        },
    ) {
        Text(
            text = glyph,
            fontFamily = LocalIconFont.current,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
