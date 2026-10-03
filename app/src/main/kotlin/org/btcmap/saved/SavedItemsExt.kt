package org.btcmap.saved

import androidx.fragment.app.Fragment
import org.btcmap.api
import org.btcmap.db
import org.btcmap.db.table.user.User as DbUser

/**
 * Fragment shims over the shared [SavedItems]. They supply this fragment's
 * [api] and [db]; the reading and writing rules live in `:shared`.
 */

suspend fun Fragment.toggleSavedArea(areaId: Long, areaName: String) {
    SavedItems.toggleArea(api(), db(), areaId, areaName)
}

suspend fun Fragment.isAreaSaved(areaId: Long): Boolean =
    SavedItems.isAreaSaved(db(), areaId)

suspend fun Fragment.toggleSavedPlace(placeId: Long, placeName: String) {
    SavedItems.togglePlace(api(), db(), placeId, placeName)
}

suspend fun Fragment.isPlaceSaved(placeId: Long): Boolean =
    SavedItems.isPlaceSaved(db(), placeId)

suspend fun Fragment.removeSavedPlace(placeId: Long): DbUser =
    SavedItems.removePlace(api(), db(), placeId)

suspend fun Fragment.removeSavedArea(areaId: Long): DbUser =
    SavedItems.removeArea(api(), db(), areaId)
