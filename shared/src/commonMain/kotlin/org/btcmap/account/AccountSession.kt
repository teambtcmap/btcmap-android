package org.btcmap.account

import org.btcmap.platform.ioDispatcher
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.toDbUser
import org.btcmap.api.updateUsername
import org.btcmap.db.Database
import org.btcmap.db.table.user.User as DbUser
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken

/**
 * Account-level operations shared by Android and the desktop: renaming the
 * account and signing out.
 */
object AccountSession {

    /**
     * Renames the account and caches the returned user, keeping the cached saved
     * lists: the username endpoint returns them empty, so replacing the user
     * wholesale would drop them from the local cache.
     */
    suspend fun changeUsername(api: Api, db: Database, name: String): DbUser {
        val updated = api.updateUsername(name).toDbUser()
        withContext(ioDispatcher) {
            val existing = db.user.select()
            db.transaction {
                db.user.delete()
                db.user.insert(
                    updated.copy(
                        savedPlaces = existing?.savedPlaces ?: updated.savedPlaces,
                        savedAreas = existing?.savedAreas ?: updated.savedAreas,
                    ),
                )
            }
        }
        return updated
    }

    /**
     * Clears the locally stored session and returns the token that was cleared,
     * so the caller can revoke it server-side best-effort, or null when there
     * was none or the stored token had already been replaced.
     *
     * The comparison is atomic with the clear (see
     * [Settings.clearSessionIfTokenMatches]), so a sign-in that raced this
     * sign-out is not dropped.
     */
    suspend fun clearSession(db: Database, settings: Settings): String? {
        val token = withContext(ioDispatcher) { settings.authToken }
        val cleared = withContext(ioDispatcher) {
            val stored = token?.takeIf { it.isNotBlank() }
            if (stored == null) {
                settings.clearSession(db)
                false
            } else {
                settings.clearSessionIfTokenMatches(db, stored)
            }
        }
        return if (cleared) token else null
    }
}
