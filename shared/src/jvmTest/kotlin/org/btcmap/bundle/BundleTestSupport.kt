package org.btcmap.bundle

import okio.Buffer
import okio.Source
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.json.btcmapJson

/** A one-shot Okio source over this JSON text or these bytes. */
internal fun String.asSource(): Source = Buffer().writeUtf8(this)

internal fun ByteArray.asSource(): Source = Buffer().write(this)

/** Decodes a single bundled record and maps it, as the old pull reader did. */
internal fun parseBundledPlace(json: String): Place =
    btcmapJson.decodeFromString<BundledPlaceJson>(json).toPlace()

internal fun parseBundledArea(json: String): Area =
    btcmapJson.decodeFromString<BundledAreaJson>(json).toArea()

internal fun parseBundledEvent(json: String): Event =
    btcmapJson.decodeFromString<BundledEventJson>(json).toEvent()

internal fun parseBundledComment(json: String): Comment =
    btcmapJson.decodeFromString<BundledCommentJson>(json).toComment()
