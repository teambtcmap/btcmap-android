package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject

/**
 * A note: free-form text its author pins to a coordinate. A note references no
 * place, area or other map entity, only its author, which makes it a fit for
 * things that are not (yet) a place and for personal reminders.
 *
 * [public] notes are readable by anyone and returned by the geo search; private
 * notes are visible only to the author.
 */
data class Note(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val text: String,
    val icon: String,
    val public: Boolean,
    val authorId: Long,
    val authorName: String,
    val createdAt: String,
    val updatedAt: String,
    /** Set only on owner responses that asked for `include_deleted=true`. */
    val deletedAt: String? = null,
)

/**
 * Creates a note owned by the authenticated user (`POST /v4/notes`). Any
 * signed-in user may create notes. The note is private unless [public] is true.
 * [icon] is the pin discriminator the server stores verbatim (a Material Symbols
 * name in this app); the API defaults it to `notes` when omitted.
 */
suspend fun Api.createNote(
    lat: Double,
    lon: Double,
    text: String,
    icon: String,
    public: Boolean,
): Note {
    val url = buildUrl("v4", "notes")

    val req = buildJsonObject {
        put("lat", lat)
        put("lon", lon)
        put("text", text)
        put("icon", icon)
        put("public", public)
    }

    return call(HttpMethod.Post, url, body = req) { body -> body.toJsonObject().toNote() }
}

/**
 * Lists the authenticated user's notes (`GET /v4/users/me/notes`), public and
 * private, for the delta sync. [updatedSince] filters to notes changed after it
 * (null reads from the beginning), [includeDeleted] also returns soft-deleted
 * notes so the client can keep their tombstones, and [limit] caps the page.
 */
suspend fun Api.getMyNotes(
    updatedSince: Instant?,
    includeDeleted: Boolean,
    limit: Long,
): List<Note> {
    val url = buildUrl("v4", "users", "me", "notes") {
        parameters.append("limit", "$limit")
        if (includeDeleted) parameters.append("include_deleted", "true")
        addUpdatedSince(updatedSince)
    }

    return call(HttpMethod.Get, url) { body -> body.toJsonArray().map { it.jsonObject.toNote() } }
}

/**
 * Changes a note's visibility (`PATCH /v4/notes/{id}`). Only the author may
 * update their note; the response is the updated note.
 */
suspend fun Api.updateNote(id: Long, public: Boolean): Note {
    val url = buildUrl("v4", "notes", "$id")

    val req = buildJsonObject {
        put("public", public)
    }

    return call(HttpMethod.Patch, url, body = req) { body -> body.toJsonObject().toNote() }
}

/**
 * Edits a note's body (`PATCH /v4/notes/{id}`). Only the author may update their
 * note; the response is the updated note.
 */
suspend fun Api.updateNoteText(id: Long, text: String): Note {
    val url = buildUrl("v4", "notes", "$id")

    val req = buildJsonObject {
        put("text", text)
    }

    return call(HttpMethod.Patch, url, body = req) { body -> body.toJsonObject().toNote() }
}

/**
 * Changes a note's icon (`PATCH /v4/notes/{id}`). Only the author may update
 * their note; the response is the updated note.
 */
suspend fun Api.updateNoteIcon(id: Long, icon: String): Note {
    val url = buildUrl("v4", "notes", "$id")

    val req = buildJsonObject {
        put("icon", icon)
    }

    return call(HttpMethod.Patch, url, body = req) { body -> body.toJsonObject().toNote() }
}

/**
 * Soft-deletes the author's own note (`DELETE /v4/notes/{id}`): the row is kept
 * with `deleted_at` set so owner clients can sync the removal, and it never
 * appears in search again. Only the author may delete their note.
 */
suspend fun Api.deleteNote(id: Long) {
    val url = buildUrl("v4", "notes", "$id")

    call(HttpMethod.Delete, url) { }
}

internal fun JsonObject.toNote(): Note {
    val author = obj("author")
    return Note(
        id = long("id"),
        lat = double("lat"),
        lon = double("lon"),
        text = string("text"),
        icon = string("icon"),
        public = boolean("public"),
        authorId = author.long("id"),
        authorName = author.string("name"),
        createdAt = string("created_at"),
        updatedAt = string("updated_at"),
        deletedAt = stringOrNull("deleted_at"),
    )
}
