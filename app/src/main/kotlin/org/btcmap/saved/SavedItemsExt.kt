package org.btcmap.saved

import androidx.fragment.app.Fragment
import org.btcmap.api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.saveArea
import org.btcmap.api.savePlace
import org.btcmap.db
import org.btcmap.db.table.user.User

suspend fun Fragment.toggleSavedArea(areaId: Long) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedAreas.any { it.asJsonObject["id"].asLong == areaId }

    if (saved) {
        api().removeSavedArea(areaId)
    } else {
        api().saveArea(areaId)
    }

    refreshUser()
}

suspend fun Fragment.toggleSavedPlace(placeId: Long) {
    val user = requireNotNull(db().user.select()) { "user is not signed in" }
    val saved = user.savedPlaces.any { it.asJsonObject["id"].asLong == placeId }

    if (saved) {
        api().removeSavedPlace(placeId)
    } else {
        api().savePlace(placeId)
    }

    refreshUser()
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
