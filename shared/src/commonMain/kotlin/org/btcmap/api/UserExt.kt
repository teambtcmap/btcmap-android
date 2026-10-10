package org.btcmap.api

import org.btcmap.db.table.user.SavedItem as DbSavedItem
import org.btcmap.db.table.user.User as DbUser

/**
 * Maps the API representation of a user to the row cached in the local
 * database. The saved places and areas are translated to their storage
 * counterparts so the transport and storage models stay independent.
 */
fun User.toDbUser(): DbUser = DbUser(
    id = id,
    name = name,
    roles = roles,
    savedPlaces = savedPlaces.map { it.toDbSavedItem() },
    savedAreas = savedAreas.map { it.toDbSavedItem() },
    geofence = geofence,
)

private fun SavedItem.toDbSavedItem(): DbSavedItem = DbSavedItem(id = id, name = name)
