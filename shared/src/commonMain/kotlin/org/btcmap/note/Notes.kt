package org.btcmap.note

import org.btcmap.platform.ioDispatcher
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.deleteNote
import org.btcmap.api.getMyNotes
import org.btcmap.api.updateNote
import org.btcmap.db.Database
import org.btcmap.db.table.note.Note
import org.btcmap.settings.Settings
import org.btcmap.settings.authorized
import org.btcmap.util.toInstant
import org.btcmap.api.Note as ApiNote

/**
 * Reads and updates the signed-in user's cached personal notes, so the app-scoped
 * sync, Android and the desktop share one definition of what "sync", "update"
 * and "delete" mean.
 *
 * The notes table is a pure cache of one owner's notes, so the sync rewrites it
 * whole: the endpoint returns every live note (public and private), which makes a
 * full replace also drop a note deleted on another device. Nothing here waits for
 * a change count; the list is small enough that the cost does not matter.
 */
object Notes {

    /** Every cached note, newest first. */
    suspend fun load(db: Database): List<Note> = withContext(ioDispatcher) { db.note.selectAll() }

    /**
     * Replaces the cached notes with the server's list. Signed out, it is a
     * no-op returning false; otherwise it returns whether the cached notes
     * changed, so the sync can announce it. A failure propagates to the caller,
     * so the sync reports it like any other step.
     */
    suspend fun sync(api: Api, db: Database, settings: Settings): Boolean {
        if (!settings.authorized) return false

        val remote = api.getMyNotes().map { it.toDbNote() }
        return withContext(ioDispatcher) {
            val cached = db.note.selectAll()
            // The endpoint's order is not guaranteed, so compare by id; a
            // re-ordered but otherwise identical list is not a change.
            if (cached.sortedBy { it.id } == remote.sortedBy { it.id }) return@withContext false

            db.transaction {
                db.note.deleteAll()
                db.note.insert(remote)
            }
            true
        }
    }

    /** Flips a cached note's visibility, updating the server and the cache. */
    suspend fun updateVisibility(api: Api, db: Database, id: Long, public: Boolean) {
        api.updateNote(id, public)
        withContext(ioDispatcher) { db.note.updateVisibility(id, public) }
    }

    /** Deletes a cached note, removing it from the server and the cache. */
    suspend fun delete(api: Api, db: Database, id: Long) {
        api.deleteNote(id)
        withContext(ioDispatcher) { db.note.delete(id) }
    }
}

private fun ApiNote.toDbNote(): Note = Note(
    id = id,
    lat = lat,
    lon = lon,
    text = text,
    public = public,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)
