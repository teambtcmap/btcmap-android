package org.btcmap.bundle

import kotlinx.serialization.json.JsonObject
import okio.Buffer
import okio.Source
import org.btcmap.api.toArea
import org.btcmap.api.toComment
import org.btcmap.api.toEvent
import org.btcmap.api.toGetAreasDeltaItem
import org.btcmap.api.toGetCommentsItem
import org.btcmap.api.toGetEventsDeltaItem
import org.btcmap.api.toGetPlacesItem
import org.btcmap.api.toPlace
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.json.btcmapJson

/** A one-shot Okio source over this JSON text or these bytes. */
internal fun String.asSource(): Source = Buffer().writeUtf8(this)

internal fun ByteArray.asSource(): Source = Buffer().write(this)

/** Decodes a single bundled record and maps it, as the importers do per row. */
internal fun parseBundledPlace(json: String): Place =
    btcmapJson.decodeFromString<JsonObject>(json).toGetPlacesItem().toPlace()

internal fun parseBundledArea(json: String): Area =
    btcmapJson.decodeFromString<JsonObject>(json).toGetAreasDeltaItem().toArea()

internal fun parseBundledEvent(json: String): Event =
    btcmapJson.decodeFromString<JsonObject>(json).toGetEventsDeltaItem().toEvent()

internal fun parseBundledComment(json: String): Comment =
    btcmapJson.decodeFromString<JsonObject>(json).toGetCommentsItem().toComment()
