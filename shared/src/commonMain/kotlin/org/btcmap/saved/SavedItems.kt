package org.btcmap.saved

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.saveArea
import org.btcmap.api.savePlace
import org.btcmap.api.toDbUser
import org.btcmap.db.Database
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User as DbUser
import org.btcmap.i18n.getLocalizedName

/**
 * Reads and updates the signed-in user's saved places and areas, so Android and
 * the desktop share one definition of what "saved", "toggle" and "remove" mean.
 *
 * A write applies the canonical id list the endpoint returns instead of
 * refetching the whole user, keeping the cached name of every known id and
 * naming a freshly added id from the caller. Only an id that is neither cached
 * nor named — a server item the cache cannot render — triggers a full refetch.
 */
object SavedItems {

    /** Whether [placeId] is among the cached saved places. */
    suspend fun isPlaceSaved(db: Database, placeId: Long): Boolean =
        withContext(Dispatchers.IO) { db.user.select() }
            ?.savedPlaces
            ?.any { it.id == placeId }
            ?: false

    /** Whether [areaId] is among the cached saved areas. */
    suspend fun isAreaSaved(db: Database, areaId: Long): Boolean =
        withContext(Dispatchers.IO) { db.user.select() }
            ?.savedAreas
            ?.any { it.id == areaId }
            ?: false

    /** Saves [placeId] when it is not saved yet, or unsaves it when it is. */
    suspend fun togglePlace(
        api: Api,
        db: Database,
        placeId: Long,
        placeName: String,
    ): DbUser {
        val user = requireUser(db)
        val saved = user.savedPlaces.any { it.id == placeId }
        val ids = if (saved) api.removeSavedPlace(placeId) else api.savePlace(placeId)

        return replaceSaved(api, db, user, placeIds = ids, addedNames = mapOf(placeId to placeName))
    }

    /** Saves [areaId] when it is not saved yet, or unsaves it when it is. */
    suspend fun toggleArea(
        api: Api,
        db: Database,
        areaId: Long,
        areaName: String,
    ): DbUser {
        val user = requireUser(db)
        val saved = user.savedAreas.any { it.id == areaId }
        val ids = if (saved) api.removeSavedArea(areaId) else api.saveArea(areaId)

        return replaceSaved(api, db, user, areaIds = ids, addedNames = mapOf(areaId to areaName))
    }

    /**
     * Removes [placeId] from the cached saved places. Applies the canonical id
     * list the endpoint returns rather than refetching the whole user.
     */
    suspend fun removePlace(api: Api, db: Database, placeId: Long): DbUser {
        val user = requireUser(db)
        return replaceSaved(api, db, user, placeIds = api.removeSavedPlace(placeId))
    }

    /** [removePlace] for areas; see it for the contract. */
    suspend fun removeArea(api: Api, db: Database, areaId: Long): DbUser {
        val user = requireUser(db)
        return replaceSaved(api, db, user, areaIds = api.removeSavedArea(areaId))
    }

    private suspend fun requireUser(db: Database): DbUser = withContext(Dispatchers.IO) {
        requireNotNull(db.user.select()) { "user is not signed in" }
    }

    /**
     * Persists [user] with its saved places and/or areas replaced by the
     * canonical id lists an endpoint returned, keeping the cached name of every
     * known id and naming a freshly added id from [addedNames]. An id that is
     * neither cached nor named means the server holds an item the cache cannot
     * render, so the whole user is refetched instead of dropping the entry.
     * Returns the cached user that now holds the result.
     */
    private suspend fun replaceSaved(
        api: Api,
        db: Database,
        user: DbUser,
        placeIds: List<Long>? = null,
        areaIds: List<Long>? = null,
        addedNames: Map<Long, String> = emptyMap(),
    ): DbUser {
        val savedPlaces = if (placeIds == null) {
            user.savedPlaces
        } else {
            user.savedPlaces.withIds(placeIds, addedNames) ?: return refreshUser(api, db)
        }
        val savedAreas = if (areaIds == null) {
            user.savedAreas
        } else {
            user.savedAreas.withIds(areaIds, addedNames) ?: return refreshUser(api, db)
        }

        val updated = user.copy(savedPlaces = savedPlaces, savedAreas = savedAreas)
        withContext(Dispatchers.IO) { db.user.insert(updated) }
        return updated
    }

    private suspend fun refreshUser(api: Api, db: Database): DbUser {
        val updated = api.getUser().toDbUser()

        withContext(Dispatchers.IO) {
            db.transaction {
                db.user.delete()
                db.user.insert(updated)
            }
        }

        return updated
    }
}

/**
 * Re-resolves each saved place's name against the local cache, which holds the
 * localized name the server omitted (the user endpoint returns the base name
 * with no `lang`). A name missing locally falls back to the server's.
 */
suspend fun List<SavedItem>.withLocalizedPlaceNames(db: Database): List<SavedItem> =
    mapLocalizedNames { db.place.selectById(it.id)?.getLocalizedName() }

/** [withLocalizedPlaceNames] for saved areas. */
suspend fun List<SavedItem>.withLocalizedAreaNames(db: Database): List<SavedItem> =
    mapLocalizedNames { db.area.selectById(it.id)?.getLocalizedName() }

private suspend fun List<SavedItem>.mapLocalizedNames(
    localizedName: suspend (SavedItem) -> String?,
): List<SavedItem> = withContext(Dispatchers.IO) {
    map { item ->
        localizedName(item)
            ?.takeIf { it.isNotBlank() }
            ?.let { item.copy(name = it) }
            ?: item
    }
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
