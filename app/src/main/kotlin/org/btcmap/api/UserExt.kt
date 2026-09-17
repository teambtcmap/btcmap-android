package org.btcmap.api

import org.btcmap.db.table.user.User as DbUser

/**
 * Maps the API representation of a user to the row cached in the local database.
 * The saved places and areas are copied over because both types use the same
 * [org.btcmap.db.table.user.SavedItem].
 */
fun User.toDbUser(): DbUser = DbUser(
    id = id,
    name = name,
    roles = roles,
    savedPlaces = savedPlaces,
    savedAreas = savedAreas,
)
