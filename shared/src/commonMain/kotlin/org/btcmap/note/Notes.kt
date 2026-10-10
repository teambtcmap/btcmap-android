package org.btcmap.note

import org.btcmap.platform.ioDispatcher
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.deleteNote
import org.btcmap.api.getMyNotes
import org.btcmap.api.updateNote
import org.btcmap.api.updateNoteIcon
import org.btcmap.api.updateNoteText
import org.btcmap.db.Database
import org.btcmap.db.table.note.Note
import org.btcmap.settings.Settings
import org.btcmap.settings.authorized
import org.btcmap.sync.syncDelta
import org.btcmap.util.toInstant
import org.btcmap.api.Note as ApiNote

/** Rows per page when pulling the note delta. Matches the endpoint's `limit` cap. */
private const val NOTES_BATCH_SIZE = 500L

/**
 * Reads and updates the signed-in user's cached personal notes, so the app-scoped
 * sync, Android and the desktop share one definition of what "sync", "update"
 * and "delete" mean.
 *
 * The notes table caches one owner's notes and syncs incrementally like the
 * other tables: `updated_since` pages the change log, soft-deleted notes are
 * kept as tombstones so the `max(updated_at)` cursor advances past a deletion,
 * and a note change elsewhere drops in on the next sync.
 */
object Notes {

    /** Every live cached note, newest first. */
    suspend fun load(db: Database): List<Note> = withContext(ioDispatcher) { db.note.selectAll() }

    /**
     * Pulls the note delta and applies it. Signed out, it is a no-op returning
     * false; otherwise it returns whether any row changed, so the sync can
     * announce it. The delta machinery reports a failed page internally rather
     * than throwing.
     */
    suspend fun sync(api: Api, db: Database, settings: Settings): Boolean {
        if (!settings.authorized) return false

        val report = syncDelta(
            baseBatchSize = NOTES_BATCH_SIZE,
            cursor = { db.note.selectMaxUpdatedAt() },
            fetch = { since, limit -> api.getMyNotes(since, includeDeleted = true, limit) },
            updatedAt = { it.updatedAt },
            apply = { rows ->
                // Tombstones come back with `deleted_at` set; they are stored
                // too, and reads exclude them, so the cursor keeps advancing.
                db.transaction { db.note.insert(rows.map { it.toDbNote() }) }
            },
        )
        return report.rowsAffected > 0
    }

    /** Flips a cached note's visibility, updating the server and the cache. */
    suspend fun updateVisibility(api: Api, db: Database, id: Long, public: Boolean) {
        api.updateNote(id, public)
        withContext(ioDispatcher) { db.note.updateVisibility(id, public) }
    }

    /** Edits a cached note's body, updating the server and the cache. */
    suspend fun updateText(api: Api, db: Database, id: Long, text: String) {
        api.updateNoteText(id, text)
        withContext(ioDispatcher) { db.note.updateText(id, text) }
    }

    /** Changes a cached note's icon, updating the server and the cache. */
    suspend fun updateIcon(api: Api, db: Database, id: Long, icon: String) {
        api.updateNoteIcon(id, icon)
        withContext(ioDispatcher) { db.note.updateIcon(id, icon) }
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
    icon = icon,
    public = public,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
