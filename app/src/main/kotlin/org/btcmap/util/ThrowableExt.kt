package org.btcmap.util

import org.btcmap.api.ApiException

/**
 * Returns a message that is safe to show to the user. Only server messages for
 * client errors are user-actionable; transport, parse and server-side failures
 * fall back to [fallback] so internal details are not exposed.
 */
fun Throwable.userFacingMessage(fallback: String): String =
    (this as? ApiException)
        ?.takeIf { it.code in 400..499 }
        ?.message
        ?.takeIf { it.isNotBlank() }
        ?: fallback
