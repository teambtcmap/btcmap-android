package org.btcmap.account

import org.btcmap.db.table.user.User

/** The roles that may manage areas. */
val AREA_ADMIN_ROLES = setOf("area_manager", "admin", "root")

/**
 * Whether [user] may manage areas. Mirrors the server's rule (`area_manager`,
 * `admin` or `root`), which stays authoritative, so a stale role cache can at
 * worst offer the screen the server then rejects.
 */
fun User.canManageAreas(): Boolean = roles.any { it in AREA_ADMIN_ROLES }
