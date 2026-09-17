package org.btcmap.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.btcmap.db.Database
import org.btcmap.db.table.user.User

internal const val KEY_AUTH_TOKEN = "auth_token"

private const val KEY_LEGACY_IMPORTED = "legacy_prefs_imported"

/**
 * Prefix used by a build that encrypted the session token with the Android
 * Keystore. Such a value can no longer be decrypted, so it is dropped on import
 * and the user is asked to sign in again.
 */
private const val LEGACY_ENCRYPTED_PREFIX = "enc:v1:"

/**
 * Settings backed by the app database instead of SharedPreferences, so they can
 * be changed in the same transaction as the other rows they belong with (for
 * example the stored session token and the cached user).
 *
 * Values are cached in memory so main-thread reads never touch the database:
 * the cache is loaded from the database on first access and every write updates
 * the cache synchronously while persisting in the background. Callers that need
 * the write to be durable before continuing (sign-in and sign-out) use
 * [replaceSession], [clearSession] and [clearSessionIfTokenMatches], which write
 * within a transaction while holding the lock.
 */
class Settings(
    private val dbProvider: () -> Database,
    private val legacyValues: () -> Map<String, Any?>,
) {
    private val lock = Any()
    private val cache = HashMap<String, String>()
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var boundDb: Database? = null

    /**
     * Returns the database the cache is loaded from, loading it first when it has
     * not been loaded yet or the provider now returns a different database.
     */
    private fun ensureLoaded(): Database {
        val db = dbProvider()
        synchronized(lock) {
            if (boundDb === db) return db

            importLegacy(db)
            cache.clear()
            cache.putAll(db.preference.selectAll())
            boundDb = db
            return db
        }
    }

    /**
     * Copies the settings written by older app versions into the database once,
     * so an upgrade keeps the user's configuration and session.
     */
    private fun importLegacy(db: Database) {
        if (db.preference.select(KEY_LEGACY_IMPORTED) != null) return

        val values = runCatching { legacyValues() }.getOrNull().orEmpty()
        db.transaction {
            for ((key, value) in values) {
                val text = when (value) {
                    is String -> value
                    is Boolean, is Int, is Long, is Float -> value.toString()
                    else -> continue
                }
                if (key == KEY_AUTH_TOKEN && text.startsWith(LEGACY_ENCRYPTED_PREFIX)) {
                    // The token can no longer be decrypted, so there is no usable
                    // session. Drop the cached account too instead of leaving it
                    // behind while signed out.
                    db.user.delete()
                    continue
                }
                db.preference.upsert(key, text)
            }
            db.preference.upsert(KEY_LEGACY_IMPORTED, "true")
        }
    }

    internal fun getString(key: String, default: String?): String? {
        ensureLoaded()
        val stored = synchronized(lock) { cache[key] }
        return stored ?: default
    }

    internal fun getBoolean(key: String, default: Boolean): Boolean {
        return getString(key, null)?.toBooleanStrictOrNull() ?: default
    }

    internal fun getInt(key: String, default: Int): Int {
        return getString(key, null)?.toIntOrNull() ?: default
    }

    internal fun getFloat(key: String, default: Float): Float {
        return getString(key, null)?.toFloatOrNull() ?: default
    }

    internal fun putString(key: String, value: String?) {
        val db = ensureLoaded()
        synchronized(lock) {
            if (value == null) cache.remove(key) else cache[key] = value
        }
        writeScope.launch {
            if (value == null) db.preference.delete(key) else db.preference.upsert(key, value)
        }
    }

    internal fun putInt(key: String, value: Int?) {
        putString(key, value?.toString())
    }

    internal fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    internal fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    /**
     * Atomically stores [token] together with [user], so a session can never be
     * left half written. Passing a null [token] clears the session.
     *
     * The transaction and the cache update are performed under [lock], which is
     * also held by [clearSessionIfTokenMatches], so a rejected token from an
     * older session can never clear the session stored here. Runs synchronously,
     * so call it off the main thread.
     */
    internal fun replaceSession(db: Database, token: String?, user: User?) {
        ensureLoaded()
        synchronized(lock) {
            db.transaction {
                if (token == null) {
                    db.preference.delete(KEY_AUTH_TOKEN)
                } else {
                    db.preference.upsert(KEY_AUTH_TOKEN, token)
                }
                db.user.delete()
                if (user != null) db.user.insert(user)
            }
            if (token == null) cache.remove(KEY_AUTH_TOKEN) else cache[KEY_AUTH_TOKEN] = token
        }
    }

    /**
     * Atomically clears the stored session token and the cached user. Runs
     * synchronously, so call it off the main thread.
     */
    internal fun clearSession(db: Database) {
        ensureLoaded()
        synchronized(lock) {
            db.transaction {
                db.preference.delete(KEY_AUTH_TOKEN)
                db.user.delete()
            }
            cache.remove(KEY_AUTH_TOKEN)
        }
    }

    /**
     * Clears the stored session token and cached user only while the stored
     * token still equals [expected]. The comparison and the clear run under the
     * same lock as [replaceSession], so a late rejected request from an old
     * session can never sign out an account that was signed in again in the
     * meantime.
     *
     * Returns true when the session was cleared. Runs synchronously, so call it
     * off the main thread.
     */
    internal fun clearSessionIfTokenMatches(db: Database, expected: String): Boolean {
        ensureLoaded()
        synchronized(lock) {
            if (cache[KEY_AUTH_TOKEN] != expected) return false

            db.transaction {
                db.preference.delete(KEY_AUTH_TOKEN)
                db.user.delete()
            }
            cache.remove(KEY_AUTH_TOKEN)
            return true
        }
    }

    /**
     * Stores [token] without touching the cached user, keeping the in-memory
     * cache and the database in sync. Only for tests that need to set up a
     * particular session state; production code must use [replaceSession] so the
     * token and the cached account stay consistent.
     */
    internal fun setAuthTokenForTesting(token: String?) {
        ensureLoaded()
        val db = dbProvider()
        synchronized(lock) {
            if (token == null) {
                db.preference.delete(KEY_AUTH_TOKEN)
                cache.remove(KEY_AUTH_TOKEN)
            } else {
                db.preference.upsert(KEY_AUTH_TOKEN, token)
                cache[KEY_AUTH_TOKEN] = token
            }
        }
    }

    /** Drops the cached values and stored settings, reloading them on next use. */
    internal fun clearForTesting() {
        ensureLoaded()
        dbProvider().preference.deleteAll()
        synchronized(lock) { cache.clear() }
        boundDb = null
    }
}
