package org.btcmap.saved

import androidx.fragment.app.Fragment
import org.btcmap.api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.saveArea
import org.btcmap.api.savePlace
import org.btcmap.api.toDbUser
import org.btcmap.db
import org.btcmap.db.table.user.SavedItem

suspend fun Fragment.toggleSavedArea(areaId: Long, areaName: String) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedAreas.any { it.id == areaId }
    val ids = if (saved) api().removeSavedArea(areaId) else api().saveArea(areaId)
    val savedAreas = user.savedAreas.withIds(ids, areaId, areaName) ?: return refreshUser()

    db().user.insert(user.copy(savedAreas = savedAreas))
}

suspend fun Fragment.isAreaSaved(areaId: Long): Boolean {
    val user = db().user.select() ?: return false
    return user.savedAreas.any { it.id == areaId }
}

suspend fun Fragment.toggleSavedPlace(placeId: Long, placeName: String) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedPlaces.any { it.id == placeId }
    val ids = if (saved) api().removeSavedPlace(placeId) else api().savePlace(placeId)
    val savedPlaces = user.savedPlaces.withIds(ids, placeId, placeName) ?: return refreshUser()

    db().user.insert(user.copy(savedPlaces = savedPlaces))
}

private fun List<SavedItem>.withIds(
    ids: List<Long>,
    addedId: Long,
    addedName: String,
): List<SavedItem>? {
    val byId = associateBy { it.id }
    val merged = mutableListOf<SavedItem>()

    for (id in ids) {
        val existing = byId[id]
        when {
            existing != null -> merged.add(existing)
            id == addedId && addedName.isNotBlank() -> merged.add(SavedItem(id, addedName))
            else -> return null
        }
    }

    return merged
}

private suspend fun Fragment.refreshUser() {
    val updated = api().getUser()

    db().transaction {
        db().user.delete()
        db().user.insert(updated.toDbUser())
    }
}
