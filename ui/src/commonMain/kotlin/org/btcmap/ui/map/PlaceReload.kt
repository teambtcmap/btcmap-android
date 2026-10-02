package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place

/**
 * Re-reads [place] from [db] whenever [reloadKey] changes, so a sync that
 * rewrites the selected row in place is reflected instead of leaving the stale
 * copy up.
 *
 * Selecting another place resets to it immediately (the state is remembered per
 * id), so there is no frame of the previous row before the re-read lands.
 */
@Composable
internal fun rememberReloadedPlace(
    place: Place?,
    db: Database,
    reloadKey: Int,
): Place? {
    var current by remember(place?.id) { mutableStateOf(place) }
    LaunchedEffect(place?.id, reloadKey) {
        val id = place?.id ?: return@LaunchedEffect
        withContext(Dispatchers.Default) { db.place.selectById(id) }?.let { current = it }
    }
    return current
}
