package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** A saved place or area row: a name and a delete action. */
data class SavedItemUi(
    val id: Long,
    val name: String,
)

/** The already-resolved strings [UserProfileScreen] renders. */
data class UserProfileLabels(
    val username: String,
    val password: String,
    val savedPlaces: String,
    val savedAreas: String,
    val noSavedPlaces: String,
    val noSavedAreas: String,
    val logOut: String,
    val editUsername: String,
    val editPassword: String,
    val delete: String,
)

data class UserProfileUiState(
    val username: String,
    val password: String,
    val savedPlaces: List<SavedItemUi>,
    val savedAreas: List<SavedItemUi>,
    val labels: UserProfileLabels,
)

@Composable
fun UserProfileScreen(
    state: UserProfileUiState,
    onEditUsername: () -> Unit,
    onEditPassword: () -> Unit,
    onDeletePlace: (id: Long) -> Unit,
    onDeleteArea: (id: Long) -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionLabel(state.labels.username)
        EditRow(state.username, state.labels.editUsername, onEditUsername)

        SectionLabel(state.labels.password)
        EditRow(state.password, state.labels.editPassword, onEditPassword)

        SectionLabel(state.labels.savedPlaces)
        if (state.savedPlaces.isEmpty()) {
            EmptyLabel(state.labels.noSavedPlaces)
        } else {
            state.savedPlaces.forEach { item ->
                SavedItemRow(item, state.labels.delete) { onDeletePlace(item.id) }
            }
        }

        SectionLabel(state.labels.savedAreas)
        if (state.savedAreas.isEmpty()) {
            EmptyLabel(state.labels.noSavedAreas)
        } else {
            state.savedAreas.forEach { item ->
                SavedItemRow(item, state.labels.delete) { onDeleteArea(item.id) }
            }
        }

        Button(
            onClick = onLogOut,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(state.labels.logOut)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )
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

@Composable
private fun SavedItemRow(item: SavedItemUi, deleteDescription: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconGlyph("delete", deleteDescription, onDelete)
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
