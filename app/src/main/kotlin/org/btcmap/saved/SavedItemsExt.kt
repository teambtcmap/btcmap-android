package org.btcmap.saved

import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.saveArea
import org.btcmap.api.savePlace
import org.btcmap.api.toDbUser
import org.btcmap.db
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User as DbUser

suspend fun Fragment.toggleSavedArea(areaId: Long, areaName: String) {
    val user = requireUser()
    val saved = user.savedAreas.any { it.id == areaId }
    val ids = if (saved) api().removeSavedArea(areaId) else api().saveArea(areaId)

    replaceSaved(user, areaIds = ids, addedNames = mapOf(areaId to areaName))
}

suspend fun Fragment.isAreaSaved(areaId: Long): Boolean {
    val user = withContext(Dispatchers.IO) { db().user.select() } ?: return false
    return user.savedAreas.any { it.id == areaId }
}

suspend fun Fragment.toggleSavedPlace(placeId: Long, placeName: String) {
    val user = requireUser()
    val saved = user.savedPlaces.any { it.id == placeId }
    val ids = if (saved) api().removeSavedPlace(placeId) else api().savePlace(placeId)

    replaceSaved(user, placeIds = ids, addedNames = mapOf(placeId to placeName))
}

suspend fun Fragment.isPlaceSaved(placeId: Long): Boolean {
    val user = withContext(Dispatchers.IO) { db().user.select() } ?: return false
    return user.savedPlaces.any { it.id == placeId }
}

/**
 * Removes [placeId] from the cached saved places. Applies the canonical id list
 * the endpoint returns rather than refetching the whole user, and returns the
 * updated cached user so the caller can re-render.
 */
suspend fun Fragment.removeSavedPlace(placeId: Long): DbUser {
    val user = requireUser()
    return replaceSaved(user, placeIds = api().removeSavedPlace(placeId))
}

/** [removeSavedPlace] for areas; see it for the contract. */
suspend fun Fragment.removeSavedArea(areaId: Long): DbUser {
    val user = requireUser()
    return replaceSaved(user, areaIds = api().removeSavedArea(areaId))
}

private suspend fun Fragment.requireUser(): DbUser = withContext(Dispatchers.IO) {
    requireNotNull(db().user.select()) { "user is not signed in" }
}

/**
 * Persists [user] with its saved places and/or areas replaced by the canonical
 * id lists an endpoint returned, keeping the cached name of every known id and
 * naming a freshly added id from [addedNames]. An id that is neither cached nor
 * named means the server holds an item the cache cannot render, so the whole
 * user is refetched instead of dropping the entry. Returns the cached user that
 * now holds the result.
 */
private suspend fun Fragment.replaceSaved(
    user: DbUser,
    placeIds: List<Long>? = null,
    areaIds: List<Long>? = null,
    addedNames: Map<Long, String> = emptyMap(),
): DbUser {
    val savedPlaces = if (placeIds == null) {
        user.savedPlaces
    } else {
        user.savedPlaces.withIds(placeIds, addedNames) ?: return refreshUser()
    }
    val savedAreas = if (areaIds == null) {
        user.savedAreas
    } else {
        user.savedAreas.withIds(areaIds, addedNames) ?: return refreshUser()
    }

    val updated = user.copy(savedPlaces = savedPlaces, savedAreas = savedAreas)
    withContext(Dispatchers.IO) { db().user.insert(updated) }
    return updated
}

/**
 * Rebuilds a cached saved list from the canonical id list an endpoint returned.
 * A known id keeps its cached name; [addedNames] names ids that are not cached
 * yet. Returns null when an id is neither cached nor named, so the caller can
 * refetch the user rather than render an entry with no name.
 */
internal fun List<SavedItem>.withIds(
    ids: List<Long>,
    addedNames: Map<Long, String> = emptyMap(),
): List<SavedItem>? {
    val byId = associateBy { it.id }
    val merged = mutableListOf<SavedItem>()

    for (id in ids) {
        val existing = byId[id]
        val name = addedNames[id]?.takeIf { it.isNotBlank() }
        when {
            existing != null -> merged.add(existing)
            name != null -> merged.add(SavedItem(id, name))
            else -> return null
        }
    }

    return merged
}

private suspend fun Fragment.refreshUser(): DbUser {
    val updated = api().getUser().toDbUser()

    withContext(Dispatchers.IO) {
        db().transaction {
            db().user.delete()
            db().user.insert(updated)
        }
    }

    return updated
}
