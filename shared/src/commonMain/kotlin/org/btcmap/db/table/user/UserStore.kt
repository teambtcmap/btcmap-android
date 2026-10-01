package org.btcmap.db.table.user

import com.google.gson.Gson
import org.btcmap.db.table.preference.PreferenceQueries

/**
 * Stores the signed-in user as a single JSON value under [KEY] in the
 * preference table.
 *
 * The account is a single value that is either present or absent, so a
 * one-row table bought nothing but a migration and an awkward "0 or 1 rows"
 * shape. Keeping it next to the session token also lets both be written in
 * one transaction.
 */
class UserStore(private val preference: PreferenceQueries) {
    private val gson = Gson()

    fun insert(user: User) {
        preference.upsert(KEY, gson.toJson(user))
    }

    fun select(): User? {
        val json = preference.select(KEY) ?: return null
        // Corrupt or pre-format JSON is treated as absent, like a missing
        // session, instead of throwing out of settings preload (which swallows
        // it) or a screen that assumes a signed-in user.
        return runCatching { gson.fromJson(json, User::class.java) }.getOrNull()
    }

    fun delete() {
        preference.delete(KEY)
    }

    companion object {
        /** The preference key the user is stored under. */
        const val KEY = "user"
    }
}
