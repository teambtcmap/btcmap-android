package org.btcmap.place

import org.btcmap.api.PlaceImage
import org.btcmap.db.table.user.User

/** The roles that may delete any place image, not only their own uploads. */
val IMAGE_MODERATOR_ROLES = setOf("admin", "root")

/**
 * Whether [user] may delete [image]: they uploaded it, or they hold a moderator
 * role. Mirrors the server's rule for
 * `DELETE /v4/places/{id}/images/{image_id}`, which remains authoritative, so a
 * stale role cache can at worst offer a delete the server then rejects.
 */
fun User.canDeletePlaceImage(image: PlaceImage): Boolean =
    image.createdBy == id || roles.any { it in IMAGE_MODERATOR_ROLES }
