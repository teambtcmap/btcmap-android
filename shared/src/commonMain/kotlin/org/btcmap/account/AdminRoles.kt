package org.btcmap.account

import org.btcmap.db.table.user.User

/** The roles that may open the infrastructure dashboard. */
val ADMIN_ROLES = setOf("admin", "root")

/**
 * Whether [user] holds an admin or root role. Mirrors the server's rule for the
 * `dashboard` RPC method, which stays authoritative.
 */
fun User.isAdmin(): Boolean = roles.any { it in ADMIN_ROLES }
