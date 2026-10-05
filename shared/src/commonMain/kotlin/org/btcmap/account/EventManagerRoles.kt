package org.btcmap.account

import org.btcmap.db.table.user.User

/** The roles that may review event submissions. */
val EVENT_MANAGER_ROLES = setOf("admin", "root", "event_manager")

/**
 * Whether [user] may list, approve or reject submitted events. Mirrors the
 * server's privileged rule (`event_manager`, `admin` or `root`), which stays
 * authoritative, so a stale role cache can at worst offer a review the server
 * then rejects with `403`.
 */
fun User.canManageEvents(): Boolean = roles.any { it in EVENT_MANAGER_ROLES }
