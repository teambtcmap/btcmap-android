package org.btcmap.saved

import androidx.fragment.app.Fragment
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.btcmap.api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.saveArea
import org.btcmap.api.savePlace
import org.btcmap.db
import org.btcmap.db.table.user.User

suspend fun Fragment.toggleSavedArea(areaId: Long, areaName: String) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedAreas.any { it.asJsonObject["id"].asLong == areaId }
    val ids = if (saved) api().removeSavedArea(areaId) else api().saveArea(areaId)
    val savedAreas = user.savedAreas.withIds(ids, areaId, areaName) ?: return refreshUser()

    db().user.insert(user.copy(savedAreas = savedAreas))
}

suspend fun Fragment.toggleSavedPlace(placeId: Long, placeName: String) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedPlaces.any { it.asJsonObject["id"].asLong == placeId }
    val ids = if (saved) api().removeSavedPlace(placeId) else api().savePlace(placeId)
    val savedPlaces = user.savedPlaces.withIds(ids, placeId, placeName) ?: return refreshUser()

    db().user.insert(user.copy(savedPlaces = savedPlaces))
}

private fun JsonArray.withIds(
    ids: List<Long>,
    addedId: Long,
    addedName: String,
): JsonArray? {
    val byId = associateBy { it.asJsonObject["id"].asLong }
    val merged = JsonArray()

    for (id in ids) {
        val existing = byId[id]
        when {
            existing != null -> merged.add(existing)
            id == addedId && addedName.isNotBlank() -> merged.add(
                JsonObject().apply {
                    addProperty("id", id)
                    addProperty("name", addedName)
                }
            )
            else -> return null
        }
    }

    return merged
}

private suspend fun Fragment.refreshUser() {
    val updated = api().getUser()

    db().transaction {
        db().user.delete()
        db().user.insert(
            User(
                id = updated.id,
                name = updated.name,
                roles = updated.roles,
                savedPlaces = updated.savedPlaces,
                savedAreas = updated.savedAreas,
            )
        )
    }
}
